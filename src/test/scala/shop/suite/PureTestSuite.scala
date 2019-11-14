package shop.suite

import cats.effect._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{ util => ju }
import org.scalatest.AsyncFunSuite
import org.scalatest.compatible.Assertion
import org.scalactic.source.Position
import scala.concurrent.ExecutionContext

trait PureTestSuite extends AsyncFunSuite {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  def randomId[A: Coercible[ju.UUID, ?]]: A = ju.UUID.randomUUID().coerce[A]

  def spec(testName: String)(f: IO[Assertion])(implicit pos: Position): Unit =
    test(testName)(f.unsafeToFuture())

}
