package suite

import cats.effect._
import weaver.scalacheck.{ CheckConfig, Checkers }
import weaver.{ Expectations, IOSuite }

abstract class ResourceSuite extends IOSuite with Checkers {

  // For it:tests, one test is enough
  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1)

  implicit class SharedResOps(res: Resource[IO, Res]) {
    def beforeAll(f: Res => IO[Unit]): Resource[IO, Res] =
      res.evalTap(f)

    def afterAll(f: Res => IO[Unit]): Resource[IO, Res] =
      for {
        p <- Resource.eval(Deferred[IO, Unit])
        x <- res.onFinalize(p.complete(()).void)
        _ <- p.get.background.onFinalize(f(x))
      } yield x
  }

  def testBeforeAfterEach(
      before: Res => IO[Unit],
      after: Res => IO[Unit]
  ): String => (Res => IO[Expectations]) => Unit =
    name => fa => test(name)(res => before(res) *> fa(res) <* after(res))

  def testAfterEach(after: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(_ => IO.unit, after)

  def testBeforeEach(before: Res => IO[Unit]): String => (Res => IO[Expectations]) => Unit =
    testBeforeAfterEach(before, _ => IO.unit)

}
