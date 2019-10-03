package shop.services

import cats.Applicative
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.category._

trait CategoryService[F[_]] {
  def getAll: F[List[Category]]
}

object LiveCategoryService {
  def make[F[_]: Applicative]: F[CategoryService[F]] =
    new LiveCategoryService[F](
      List("Guitars").map(_.coerce[Category])
    ).pure[F].widen
}

class LiveCategoryService[F[_]: Applicative] private (
    categories: List[Category]
) extends CategoryService[F] {
  def getAll: F[List[Category]] = categories.pure[F]
}
