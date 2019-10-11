package shop.algebras

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.brand.Brand
import shop.domain.item._

trait Items[F[_]] {
  def getAll: F[List[Item]]
  def findBy(brand: Brand): F[List[Item]]
  def create(item: Item): F[Unit]
  def update(item: Item): F[Unit]
}

object LiveItems {
  def make[F[_]: Applicative: GenUUID]: F[Items[F]] =
    GenUUID[F].make.replicateA(2).map { uuids =>
      val items = uuids
        .map(_.coerce[ItemId])
        .zip(
          List(
            (ItemName("Schecter"), ItemDescription("Electric Guitar"), USD(343)),
            (ItemName("MusicMan"), ItemDescription("Electric Guitar"), USD(555))
          )
        )
        .map { case (id, (n, d, p)) => Item(id, n, d, p) }
      new LiveItems(items)
    }
}

class LiveItems[F[_]: Applicative] private (
    items: List[Item]
) extends Items[F] {
  def getAll: F[List[Item]]               = items.pure[F]
  def findBy(brand: Brand): F[List[Item]] = items.pure[F]
  def create(item: Item): F[Unit]         = ().pure[F]
  def update(item: Item): F[Unit]         = ().pure[F]
}
