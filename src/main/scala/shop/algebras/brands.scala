package shop.algebras

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.brand._

trait Brands[F[_]] {
  def getAll: F[List[Brand]]
  def create(brand: Brand): F[Unit]
}

object LiveBrands {
  def make[F[_]: Applicative]: F[Brands[F]] =
    new LiveBrands[F](
      List("Gibson", "Ibanez", "Schecter").map(_.coerce[Brand])
    ).pure[F].widen
}

class LiveBrands[F[_]: Applicative] private (
    brands: List[Brand]
) extends Brands[F] {
  def getAll: F[List[Brand]]        = brands.pure[F]
  def create(brand: Brand): F[Unit] = ().pure[F]
}
