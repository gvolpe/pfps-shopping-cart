package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.database._
import shop.domain.brand.Brand
import shop.domain.category.Category
import shop.domain.item._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: Brand): F[List[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: Item): F[Unit]
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

  def findBy(brand: Brand): F[List[Item]] =
    session.prepare(selectByBrand).use { ps =>
      ps.stream(brand, 1024).compile.toList
    }

  def create(item: CreateItem): F[Unit] =
    session.prepare(insertBrand).use { cmd =>
      GenUUID[F].make.flatMap { uuid =>
        cmd.execute(item.toItem(uuid.coerce[ItemId])).void
      }
    }

  def update(item: Item): F[Unit] =
    session.prepare(updateBrand).use { cmd =>
      cmd.execute(item.name.value ~ item.uuid.value.toString).void
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
          b.coerce[Brand],
          c.coerce[Category]
        )
    }(
      i => i.uuid.value.toString ~ i.name.value ~ i.description.value ~ i.price.value ~ i.brand.value ~ i.category.value
    )

  val selectAll: Query[Void, Item] =
    sql"""
        SELECT * FROM items
       """.query(itemCodec)

  val selectByBrand: Query[Brand, Item] =
    sql"""
        SELECT * FROM items
        WHERE brand LIKE ${coercibleVarchar[Brand]}
       """.query(itemCodec)

  val insertBrand: Command[Item] =
    sql"""
        INSERT INTO items
        VALUES ($itemCodec)
       """.command

  val updateBrand: Command[String ~ String] =
    sql"""
        UPDATE items
        SET name = $varchar
        WHERE uuid = $varchar
       """.command

}
