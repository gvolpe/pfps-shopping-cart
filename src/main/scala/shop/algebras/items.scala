package shop.algebras

import cats.Applicative
import cats.effect._
import cats.implicits._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.brand.Brand
import shop.domain.category.Category
import shop.domain.item._

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: Brand): F[List[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: Item): F[Unit]
}

object LiveItems {
  def make[F[_]: Applicative](
      session: Session[F]
  ): F[Items[F]] =
    new LiveItems[F](session).pure[F].widen
}

class LiveItems[F[_]] private (
    session: Session[F]
) extends Items[F] {

  val item: Decoder[Item] =
    (varchar ~ varchar ~ varchar ~ numeric ~ varchar ~ varchar).map {
      case i ~ n ~ d ~ p ~ b ~ c =>
        Item(
          ju.UUID.fromString(i).coerce[ItemId],
          n.coerce[ItemName],
          d.coerce[ItemDescription],
          p.coerce[USD],
          b.coerce[Brand],
          c.coerce[Category]
        )
    }

  def findAll: F[List[Item]] =
    session.execute(
      sql"""
        select * from items
      """.query(item)
    )

  def findBy(brand: Brand): F[List[Item]] = ???
  def create(item: CreateItem): F[Unit]   = ???
  def update(item: Item): F[Unit]         = ???
}
