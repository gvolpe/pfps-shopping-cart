package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.brand.Brand
import shop.domain.category._
import shop.domain.brand._
import shop.domain.item._
import shop.ext.skunkx._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: UpdateItem): F[Unit]
}

object LiveItems {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Items[F]] =
    Sync[F].delay(
      new LiveItems[F](sessionPool)
    )
}

final class LiveItems[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]]
) extends Items[F] {
  import ItemQueries._

  // In the book we'll see how to retrieve results in chunks using stream or cursor
  def findAll: F[List[Item]] =
    sessionPool.use(_.execute(selectAll))

  def findBy(brand: BrandName): F[List[Item]] =
    sessionPool.use { session =>
      session.prepare(selectByBrand).use { ps =>
        ps.stream(brand, 1024).compile.toList
      }
    }

  def findById(itemId: ItemId): F[Option[Item]] =
    sessionPool.use { session =>
      session.prepare(selectById).use { ps =>
        ps.option(itemId)
      }
    }

  def create(item: CreateItem): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertItem).use { cmd =>
        GenUUID[F].make[ItemId].flatMap { id =>
          cmd.execute(id ~ item).void
        }
      }
    }

  def update(item: UpdateItem): F[Unit] =
    sessionPool.use { session =>
      session.prepare(updateItem).use { cmd =>
        cmd.execute(item).void
      }
    }

}

private object ItemQueries {

  val decoder: Decoder[Item] =
    (uuid ~ varchar ~ varchar ~ numeric ~ uuid ~ varchar ~ uuid ~ varchar).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          i.coerce[ItemId],
          n.coerce[ItemName],
          d.coerce[ItemDescription],
          p.coerce[USD],
          Brand(bi.coerce[BrandId], bn.coerce[BrandName]),
          Category(ci.coerce[CategoryId], cn.coerce[CategoryName])
        )
    }

  val selectAll: Query[Void, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
       """.query(decoder)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
        WHERE b.name LIKE ${varchar.cimap[BrandName]}
       """.query(decoder)

  val selectById: Query[ItemId, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
        WHERE i.uuid = ${uuid.cimap[ItemId]}
       """.query(decoder)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
        INSERT INTO items
        VALUES ($uuid, $varchar, $varchar, $numeric, $uuid, $uuid)
       """.command.contramap {
      case id ~ i =>
        id.value ~ i.name.value ~ i.description.value ~ i.price.value ~ i.brandId.value ~ i.categoryId.value
    }

  val updateItem: Command[UpdateItem] =
    sql"""
        UPDATE items
        SET price = $numeric
        WHERE uuid = ${uuid.cimap[ItemId]}
       """.command.contramap(i => i.price.value ~ i.id)

}
