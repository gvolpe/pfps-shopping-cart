package shop

import cats.effect.IO
import org.scalatest.compatible.Assertion
import scala.concurrent.Future

object IOAssertion {
  def apply(ioa: IO[Assertion]): Future[Assertion] = ioa.unsafeToFuture()
}

