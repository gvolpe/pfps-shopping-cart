package shop.suite

import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.scalatest.compatible.Assertion

trait HttpTestSuite extends PureTestSuite {

  def assertHttp(routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: Status,
      expectedBody: String
  ) =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.bodyAsText.compile.foldMonoid.map { json =>
          assert(resp.status == expectedStatus && json == expectedBody)
        }
      case None => fail("route nout found")
    }

  def assertHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status) =
    routes.run(req).value.map {
      case Some(resp) =>
        assert(resp.status == expectedStatus)
      case None => fail("route nout found")
    }

  def assertHttpFailure(routes: HttpRoutes[IO], req: Request[IO]) =
    routes.run(req).value.attempt.map {
      case Left(_)  => assert(true)
      case Right(_) => fail("expected a failure")
    }

}
