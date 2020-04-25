package suite

import cats.effect._
import munit.ScalaCheckSuite
import scala.concurrent.ExecutionContext

trait PureTestSuite extends ScalaCheckSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

}
