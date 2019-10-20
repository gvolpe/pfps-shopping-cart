package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.database._
import shop.domain.brand.Brand
import shop.domain.category._
import shop.domain.brand._
import shop.domain.item._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: UpdateItem): F[Unit]
}

object LiveItems {
  def make[F[_]: Sync](
      session: Session[F]
  ): F[Items[F]] =
    new LiveItems[F](session).pure[F].widen
}

class LiveItems[F[_]: Sync] private (
    session: Session[F]
) extends Items[F] {
  import ItemQueries._

  def findAll: F[List[Item]] =
    session.execute(selectAll)

  def findBy(brand: BrandName): F[List[Item]] =
    session.prepare(selectByBrand).use { ps =>
      ps.stream(brand, 1024).compile.toList
    }

  def create(item: CreateItem): F[Unit] =
    session.prepare(insertItem).use { cmd =>
      GenUUID[F].make[ItemId].flatMap { id =>
        cmd.execute(id ~ item).void
      }
    }

  def update(item: UpdateItem): F[Unit] =
    session.prepare(updateItem).use { cmd =>
      cmd.execute(item).void
    }

}

private object ItemQueries {

  val itemCodec: Codec[Item] =
    (varchar ~ varchar ~ varchar ~ numeric ~ varchar ~ varchar ~ varchar ~ varchar).imap {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          ju.UUID.fromString(i).coerce[ItemId],
          n.coerce[ItemName],
          d.coerce[ItemDescription],
          p.coerce[USD],
          Brand(ju.UUID.fromString(bi).coerce[BrandId], bn.coerce[BrandName]),
          Category(ju.UUID.fromString(ci).coerce[CategoryId], cn.coerce[CategoryName])
        )
    }(
      i =>
        i.uuid.value.toString ~ i.name.value ~ i.description.value ~ i.price.value ~ i.brand.uuid.value.toString ~ i.brand.name.value ~ i.category.uuid.value.toString ~ i.category.name.value
    )

  val selectAll: Query[Void, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i, brands AS b, categories AS c
        WHERE i.brand_id = b.uuid AND i.category_id = c.uuid
       """.query(itemCodec)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i, brands AS b, categories AS c
        WHERE i.brand_id = b.uuid AND i.category_id = c.uuid
        AND b.name LIKE ${coercibleVarchar[BrandName]}
       """.query(itemCodec)

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
