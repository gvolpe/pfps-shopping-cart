package shop.typeclasses

import cats.effect.Concurrent
import cats.effect.implicits._
import cats.implicits._

// Runs an arbitrary action in the background
// TODO: Define laws? or make it a simple interface
trait Background[F[_]] {
  def run[A](fa: F[A]): F[Unit]
}

object Background {
  def apply[F[_]](implicit ev: Background[F]): Background[F] = ev

  implicit def concurrentBackground[F[_]: Concurrent]: Background[F] =
    new Background[F] {
      def run[A](fa: F[A]): F[Unit] = fa.start.void
    }
}
