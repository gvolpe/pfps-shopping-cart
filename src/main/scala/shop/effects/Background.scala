package shop.effects

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import scala.concurrent.duration.FiniteDuration

// Runs an arbitrary action in the background
// TODO: Define laws? or make it a simple interface
trait Background[F[_]] {
  def schedule[A](
      duration: FiniteDuration,
      fa: F[A]
  ): F[Unit]
}

object Background {
  def apply[F[_]](implicit ev: Background[F]): Background[F] = ev

  implicit def concurrentBackground[F[_]: Concurrent: Timer]: Background[F] =
    new Background[F] {

      def schedule[A](
          duration: FiniteDuration,
          fa: F[A]
      ): F[Unit] =
        (Timer[F].sleep(duration) *> fa).start.void

    }

}
