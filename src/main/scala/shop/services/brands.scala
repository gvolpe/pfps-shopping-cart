package shop.services

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.brand._

trait BrandService[F[_]] {
  def getAll: F[List[Brand]]
  def create(brand: Brand): F[Unit]
}

object LiveBrandService {
  def make[F[_]: Applicative]: F[BrandService[F]] =
    new LiveBrandService[F](
      List("Gibson", "Ibanez", "Schecter").map(_.coerce[Brand])
    ).pure[F].widen
}

class LiveBrandService[F[_]: Applicative] private (
    brands: List[Brand]
) extends BrandService[F] {
  def getAll: F[List[Brand]]        = brands.pure[F]
  def create(brand: Brand): F[Unit] = ().pure[F]
}
