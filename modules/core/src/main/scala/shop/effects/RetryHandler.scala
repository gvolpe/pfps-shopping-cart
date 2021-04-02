package shop.effects

import org.typelevel.log4cats.Logger
import retry.RetryDetails._
import retry._

trait RetryHandler[F[_]] {
  def onError(action: String)(e: Throwable, details: RetryDetails): F[Unit]
}

object RetryHandler {
  def apply[F[_]: RetryHandler]: RetryHandler[F] = implicitly

  implicit def forLogger[F[_]: Logger]: RetryHandler[F] =
    new RetryHandler[F] {
      def onError(action: String)(e: Throwable, details: RetryDetails): F[Unit] =
        details match {
          case r: WillDelayAndRetry =>
            Logger[F].error(
              s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
            )
          case g: GivingUp =>
            Logger[F].error(s"Giving up on $action after ${g.totalRetries} retries.")
        }
    }
}
