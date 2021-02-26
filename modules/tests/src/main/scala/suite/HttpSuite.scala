package suite

import scala.util.control.NoStackTrace

import cats.effect.IO
import cats.implicits._
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

trait HttpSuite extends SimpleIOSuite with Checkers {

  case object DummyError extends NoStackTrace

  def assertHttp[A: Encoder](routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: Status,
      expectedBody: A
  ) =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          expect.all(resp.status === expectedStatus, json.dropNullValues === expectedBody.asJson.dropNullValues)
        }
      case None => IO.pure(failure("route nout found"))
    }

  def assertHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status) =
    routes.run(req).value.map {
      case Some(resp) => expect.same(resp.status, expectedStatus)
      case None       => failure("route nout found")
    }

  def assertHttpFailure(routes: HttpRoutes[IO], req: Request[IO]) =
    routes.run(req).value.attempt.map {
      case Left(_)  => success
      case Right(_) => failure("expected a failure")
    }

}
