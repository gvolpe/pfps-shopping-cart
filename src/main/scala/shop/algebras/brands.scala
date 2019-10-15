package shop.algebras

import cats.Applicative
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import io.estatico.newtype.ops._
import shop.domain.brand._
import shop.effects._

trait Brands[F[_]] {
  def getAll: F[List[Brand]]
  def create(brand: Brand): F[Unit]
}

object LiveBrands {
  def make[F[_]: ApThrow]: F[Brands[F]] =
    List("Gibson", "Ibanez", "Schecter")
      .traverse(b => refineV[NonEmpty](b).leftMap(InvalidBrand(_)).liftTo[F])
      .map { brands =>
        new LiveBrands[F](brands.map(_.coerce[Brand]))
      }
}

class LiveBrands[F[_]: Applicative] private (
    brands: List[Brand]
) extends Brands[F] {
  def getAll: F[List[Brand]]        = brands.pure[F]
  def create(brand: Brand): F[Unit] = ().pure[F]
}
