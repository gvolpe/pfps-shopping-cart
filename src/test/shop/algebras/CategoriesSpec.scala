package shop.algebras

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.category._

object TestCategories {
  def make[F[_]: Applicative]: F[Categories[F]] =
    new TestCategories[F](
      List("Guitars").map(_.coerce[Category])
    ).pure[F].widen
}

class TestCategories[F[_]: Applicative] private (
    categories: List[Category]
) extends Categories[F] {
  def findAll: F[List[Category]]          = categories.pure[F]
  def create(name: CategoryName): F[Unit] = ().pure[F]
}
