package shop.algebras

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.category._

trait Categories[F[_]] {
  def getAll: F[List[Category]]
  def create(category: Category): F[Unit]
}

object LiveCategories {
  def make[F[_]: Applicative]: F[Categories[F]] =
    new LiveCategories[F](
      List("Guitars").map(_.coerce[Category])
    ).pure[F].widen
}

class LiveCategories[F[_]: Applicative] private (
    categories: List[Category]
) extends Categories[F] {
  def getAll: F[List[Category]]           = categories.pure[F]
  def create(category: Category): F[Unit] = ().pure[F]
}
