package shop.services

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.item._

trait ItemService[F[_]] {
  def getAll: F[List[Item]]
}

object LiveItemService {
  def make[F[_]: Applicative: GenUUID]: F[ItemService[F]] =
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
      new LiveItemService(items)
    }
}

class LiveItemService[F[_]: Applicative] private (
  items: List[Item]
) extends ItemService[F] {
  def getAll: F[List[Item]] = items.pure[F]
}
