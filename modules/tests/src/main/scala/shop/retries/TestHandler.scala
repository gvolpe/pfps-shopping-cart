package shop.retries

import scala.reflect.ClassTag

import cats.effect.IO
import cats.effect.kernel.Ref
import retry.RetryDetails._
import retry._

object TestHandler {

  private[retries] def handlerFor[A <: RetryDetails: ClassTag](ref: Ref[IO, Option[A]]): RetryHandler[IO] =
    new RetryHandler[IO] {
      def onError(retriable: Retriable)(e: Throwable, details: RetryDetails): IO[Unit] =
        details match {
          case a: A => ref.set(Some(a))
          case _    => IO.unit
        }

      def retry[T](policy: RetryPolicy[IO], retriable: Retriable)(fa: IO[T]): IO[T] =
        retryingOnAllErrors[T](policy, onError(retriable))(fa)
    }

  def givingUp(ref: Ref[IO, Option[GivingUp]]): RetryHandler[IO] =
    handlerFor[GivingUp](ref)

  def recovering(ref: Ref[IO, Option[WillDelayAndRetry]]): RetryHandler[IO] =
    handlerFor[WillDelayAndRetry](ref)

}
