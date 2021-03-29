package shop.effects

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._
import cats.effect.Temporal

trait Background[F[_]] {
  def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit]
}

object Background {
  def apply[F[_]](implicit ev: Background[F]): Background[F] = ev

  implicit def concurrentBackground[F[_]: Concurrent: Temporal]: Background[F] =
    new Background[F] {
      def schedule[A](fa: F[A], duration: FiniteDuration): F[Unit] =
        (Temporal[F].sleep(duration) *> fa).start.void
    }
}
