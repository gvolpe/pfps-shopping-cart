package shop.effects

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._

trait Background[F[_]] {
  def schedule[A](
      fa: F[A],
      duration: FiniteDuration
  ): F[Unit]
}

// TODO: Replace for the new builtin background based on Resource
object Background {
  def apply[F[_]](implicit ev: Background[F]): Background[F] = ev

  implicit def concurrentBackground[F[_]: Concurrent: Timer]: Background[F] =
    new Background[F] {

      def schedule[A](
          fa: F[A],
          duration: FiniteDuration
      ): F[Unit] =
        (Timer[F].sleep(duration) *> fa).start.void

    }

}
