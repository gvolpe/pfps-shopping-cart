package shop

import cats.effect._
import org.scalatest.AsyncFunSuite
import org.scalatest.compatible.Assertion
import org.scalactic.source.Position
import scala.concurrent.ExecutionContext

trait PureTestSuite extends AsyncFunSuite {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  def spec(testName: String)(f: IO[Assertion])(implicit pos: Position): Unit =
    test(testName)(f.unsafeToFuture())

}
