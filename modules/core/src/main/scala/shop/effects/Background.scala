package shop.effects

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import scala.concurrent.duration.FiniteDuration
import fs2.concurrent.Queue

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
        (Timer[F].sleep(duration) *> fa).start.bracket(_ => Concurrent[F].never[Unit])(_.cancel)

    }

  def safeBackground[F[_]: Concurrent: Timer]: Resource[F, Background[F]] = Resource.suspend {
    Queue.unbounded[F, (FiniteDuration, F[Any])].map { q =>
      val bg = new Background[F] {
        def schedule[A](
            fa: F[A],
            duration: FiniteDuration
        ): F[Unit] =
          q.enqueue1(duration -> fa.widen)
      }

      val backgroundStream = q.dequeue.map {
        case (duration, job) => fs2.Stream.eval_(job).delayBy(duration)
      }.parJoinUnbounded

      Resource
        .make(backgroundStream.compile.drain.start)(_.cancel)
        .as(bg)
    }
  }
}
