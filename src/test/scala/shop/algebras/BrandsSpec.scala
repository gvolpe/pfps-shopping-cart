//package shop.algebras

//import cats.Applicative
//import cats.implicits._
//import io.estatico.newtype.ops._
//import shop.domain.brand._

//object TestBrands {
//  def make[F[_]: Applicative]: F[Brands[F]] =
//    new LiveBrands[F](
//      List("Gibson", "Ibanez", "Schecter").map(_.coerce[Brand])
//    ).pure[F].widen
//}

//class TestBrands[F[_]: Applicative] private (
//    brands: List[Brand]
//) extends Brands[F] {
//  def findAll: F[List[Brand]]          = brands.pure[F]
//  def create(name: BrandName): F[Unit] = ().pure[F]
//}
