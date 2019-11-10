package shop

import cats.effect._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{util => ju}
import natchez.Trace.Implicits.noop // needed for skunk
import org.scalatest.AsyncFunSuite
import org.scalatest.compatible.Assertion
import org.scalactic.source.Position
import scala.concurrent.ExecutionContext
import skunk._

trait ItTestSuite extends AsyncFunSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

  def spec(testName: String)(f: IO[Assertion])(implicit pos: Position): Unit =
    test(testName)(f.unsafeToFuture())

  def randomId[A: Coercible[ju.UUID, ?]]: A = ju.UUID.randomUUID().coerce[A]

  val sessionPool =
    Session.pooled[IO](
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "store",
      max = 10
    )

}
