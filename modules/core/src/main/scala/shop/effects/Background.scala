package shop.effects

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._

trait Background[F[_]] {
  def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit]
}

object Background {
  def apply[F[_]: Background]: Background[F] = implicitly

  implicit def concurrentBackground[F[_]: Temporal]: Background[F] =
    new Background[F] {
      def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit] =
        (Temporal[F].sleep(duration) *> fa).start.void
    }
}
