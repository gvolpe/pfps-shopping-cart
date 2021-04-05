package suite

import cats.effect._
import weaver.IOSuite
import weaver.scalacheck.{ CheckConfig, Checkers }

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

}
