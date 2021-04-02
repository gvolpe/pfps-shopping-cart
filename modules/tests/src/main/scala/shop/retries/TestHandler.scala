package shop.retries

import cats.effect.IO
import cats.effect.kernel.Ref
import retry.RetryDetails._
import retry._

object TestHandler {

  def givingUp(ref: Ref[IO, Option[GivingUp]]): RetryHandler[IO] =
    new RetryHandler[IO] {
      def onError(retriable: Retriable)(e: Throwable, details: RetryDetails): IO[Unit] =
        details match {
          case g: GivingUp => ref.set(Some(g))
          case _           => IO.unit
        }
    }

  def recovering(ref: Ref[IO, List[WillDelayAndRetry]]): RetryHandler[IO] =
    new RetryHandler[IO] {
      def onError(retriable: Retriable)(e: Throwable, details: RetryDetails): IO[Unit] =
        details match {
          case w: WillDelayAndRetry => ref.update(_ :+ w)
          case _                    => IO.unit
        }
    }

}
