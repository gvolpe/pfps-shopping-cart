package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
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
    new LiveItems[F](sessionPool).pure[F].widen
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
    (varchar ~ varchar ~ varchar ~ numeric ~ varchar ~ varchar ~ varchar ~ varchar).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          ju.UUID.fromString(i).coerce[ItemId],
          n.coerce[ItemName],
          d.coerce[ItemDescription],
          p.coerce[USD],
          Brand(ju.UUID.fromString(bi).coerce[BrandId], bn.coerce[BrandName]),
          Category(ju.UUID.fromString(ci).coerce[CategoryId], cn.coerce[CategoryName])
        )
    }

  val selectAll: Query[Void, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i, brands AS b, categories AS c
        WHERE i.brand_id = b.uuid AND i.category_id = c.uuid
       """.query(decoder)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i, brands AS b, categories AS c
        WHERE i.brand_id = b.uuid AND i.category_id = c.uuid
        AND b.name LIKE ${coercibleVarchar[BrandName]}
       """.query(decoder)

  val selectById: Query[ItemId, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i, brands AS b, categories AS c
        WHERE i.uuid = ${coercibleUuid[ItemId]}
        AND i.brand_id = b.uuid AND i.category_id = c.uuid
       """.query(decoder)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
        INSERT INTO items
        VALUES ($varchar, $varchar, $varchar, $numeric, $varchar, $varchar)
       """.command.contramap {
      case id ~ i =>
        id.value.toString ~ i.name.value ~ i.description.value ~ i.price.value ~ i.brandId.value.toString ~ i.categoryId.value.toString
    }

  val updateItem: Command[UpdateItem] =
    sql"""
        UPDATE items
        SET price = $numeric
        WHERE uuid = $varchar
       """.command.contramap(i => i.price.value ~ i.id.value.toString)

}
