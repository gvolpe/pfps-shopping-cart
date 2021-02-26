package suite

import scala.concurrent.ExecutionContext

import cats.effect._
import munit.ScalaCheckSuite

// TODO: replace it for weaver or munit-cats-effect?
trait PureTestSuite extends ScalaCheckSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

}
