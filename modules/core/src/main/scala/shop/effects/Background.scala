package shop.effects

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.effect.implicits._
import cats.implicits._
import scala.concurrent.duration.FiniteDuration

trait Background[F[_]] {
  def schedule[A](
      fa: F[A],
      duration: FiniteDuration
  ): F[Unit]
}

object Background {
  def apply[F[_]](implicit ev: Background[F]): Background[F] = ev

  implicit def concurrentBackground[F[_]: Concurrent: Timer]: Background[F] =
    new Background[F] {

      def schedule[A](
          fa: F[A],
          duration: FiniteDuration
      ): F[Unit] =
        Deferred[F, Unit].flatMap { gate =>
          (Timer[F].sleep(duration) *> fa.guarantee(gate.complete(()))).start
            .bracket(_ => gate.get)(_.cancel)
        }

    }

}
