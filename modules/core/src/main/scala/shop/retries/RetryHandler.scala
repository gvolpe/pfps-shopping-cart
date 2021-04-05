package shop.retries

import cats.effect.Temporal
import cats.syntax.show._
import org.typelevel.log4cats.Logger
import retry.RetryDetails._
import retry._

trait RetryHandler[F[_]] {
  def onError(retriable: Retriable)(e: Throwable, details: RetryDetails): F[Unit]
  def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A]
}

object RetryHandler {
  def apply[F[_]: RetryHandler]: RetryHandler[F] = implicitly

  implicit def forLoggerTemporal[F[_]: Logger: Temporal]: RetryHandler[F] =
    new RetryHandler[F] {
      def onError(retriable: Retriable)(e: Throwable, details: RetryDetails): F[Unit] =
        details match {
          case WillDelayAndRetry(_, retriesSoFar, _) =>
            Logger[F].error(
              s"Failed to process ${retriable.show} with ${e.getMessage}. So far we have retried $retriesSoFar times."
            )
          case GivingUp(totalRetries, _) =>
            Logger[F].error(s"Giving up on ${retriable.show} after $totalRetries retries.")
        }

      def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A] =
        retryingOnAllErrors[A](policy, onError(retriable))(fa)
    }
}
