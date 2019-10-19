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

class LiveItems[F[_]: GenUUID: Sync] private (
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
      GenUUID[F].make.flatMap { uuid =>
        cmd.execute(item.toItem(uuid.coerce[ItemId])).void
      }
    }

  def update(item: UpdateItem): F[Unit] =
    session.prepare(updateItem).use { cmd =>
      cmd.execute(item.price.value ~ item.id.value.toString).void
    }

}

private object ItemQueries {

  val itemCodec: Codec[Item] =
    (varchar ~ varchar ~ varchar ~ numeric ~ varchar ~ varchar).imap {
      case i ~ n ~ d ~ p ~ b ~ c =>
        Item(
          ju.UUID.fromString(i).coerce[ItemId],
          n.coerce[ItemName],
          d.coerce[ItemDescription],
          p.coerce[USD],
          Brand(ju.UUID.fromString(b).coerce[BrandId], "foo".coerce[BrandName]),
          Category(ju.UUID.fromString(c).coerce[CategoryId], "cat".coerce[CategoryName])
        )
    }(
      i =>
        i.uuid.value.toString ~ i.name.value ~ i.description.value ~ i.price.value ~ i.brand.uuid.value.toString ~ i.category.uuid.value.toString
    )

  val selectAll: Query[Void, Item] =
    sql"""
        SELECT * FROM items
       """.query(itemCodec)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
        SELECT * FROM items
        WHERE brand LIKE ${coercibleVarchar[BrandName]}
       """.query(itemCodec)

  val insertItem: Command[Item] =
    sql"""
        INSERT INTO items
        VALUES ($itemCodec)
       """.command

  val updateItem: Command[BigDecimal ~ String] =
    sql"""
        UPDATE items
        SET price = $numeric
        WHERE uuid = $varchar
       """.command

}
