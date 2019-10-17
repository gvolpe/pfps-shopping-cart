package shop.algebras

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
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
  def make[F[_]: Applicative: GenUUID]: F[Items[F]] =
    GenUUID[F].make.replicateA(2).map { uuids =>
      val items = uuids
        .map(_.coerce[ItemId])
        .zip(
          List(
            (
              ItemName("Prog Power"),
              ItemDescription("Electric Guitar"),
              USD(343),
              Brand("Schecter"),
              Category("Guitars")
            ),
            (
              ItemName("Petrucci Signature"),
              ItemDescription("Electric Guitar"),
              USD(555),
              Brand("MusicMan"),
              Category("Guitars")
            )
          )
        )
        .map { case (id, (n, d, p, b, c)) => Item(id, n, d, p, b, c) }
      new LiveItems(items)
    }
}

class LiveItems[F[_]: Applicative] private (
    items: List[Item]
) extends Items[F] {
  def findAll: F[List[Item]] = items.pure[F]
  def findBy(brand: Brand): F[List[Item]] =
    items.filter(_.brand.value.toLowerCase == brand.value.toLowerCase).pure[F]
  def create(item: CreateItem): F[Unit] = ().pure[F]
  def update(item: Item): F[Unit]       = ().pure[F]
}
