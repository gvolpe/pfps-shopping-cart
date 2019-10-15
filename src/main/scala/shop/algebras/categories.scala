package shop.algebras

import cats.Applicative
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import io.estatico.newtype.ops._
import shop.domain.category._
import shop.effects._

trait Categories[F[_]] {
  def getAll: F[List[Category]]
  def create(category: Category): F[Unit]
}

object LiveCategories {
  def make[F[_]: ApThrow]: F[Categories[F]] =
    List("Guitars")
      .traverse(refineV[NonEmpty](_).leftMap(InvalidCategory(_)).liftTo[F])
      .map { kats =>
        new LiveCategories[F](kats.map(_.coerce[Category]))
      }
}

class LiveCategories[F[_]: Applicative] private (
    categories: List[Category]
) extends Categories[F] {
  def getAll: F[List[Category]]           = categories.pure[F]
  def create(category: Category): F[Unit] = ().pure[F]
}
