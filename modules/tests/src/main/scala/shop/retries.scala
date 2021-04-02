package shop

import shop.effects.RetryHandler

import cats.effect.IO
import cats.effect.kernel.Ref
import retry.RetryDetails._
import retry._

object retries {

  def handler(ref: Ref[IO, Option[GivingUp]]): RetryHandler[IO] =
    new RetryHandler[IO] {
      def onError(action: String)(e: Throwable, details: RetryDetails): IO[Unit] =
        details match {
          case g: GivingUp => ref.set(Some(g))
          case _           => IO.unit
        }
    }

}
