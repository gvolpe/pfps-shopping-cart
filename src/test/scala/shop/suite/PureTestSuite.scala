package shop.suite

import cats.effect._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.util.UUID
import org.scalatest.AsyncFunSuite
import org.scalatest.compatible.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalactic.source.Position
import scala.concurrent.ExecutionContext

trait PureTestSuite extends AsyncFunSuite with ScalaCheckPropertyChecks {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  def randomId[A: Coercible[UUID, ?]]: A = UUID.randomUUID().coerce[A]

  def spec(testName: String)(f: IO[Assertion])(implicit pos: Position): Unit =
    test(testName)(f.unsafeToFuture())

}
