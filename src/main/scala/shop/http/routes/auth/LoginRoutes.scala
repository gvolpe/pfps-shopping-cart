package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.auth._
import shop.http.json._
import shop.http.json.protocol._
import shop.services.AuthService
import shop.http.auth.roles._

final case class LoginRoutes[F[_]: Sync](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    // login existing user
    case req @ POST -> Root / "login" =>
      // TODO: Handle 422 code and return 400 instead?
      req.decode[LoginUser] { user =>
        authService
          .login(user.username, user.password)
          .flatMap(Ok(_))
          .handleErrorWith {
            case InvalidUserOrPassword(_) => Forbidden()
          }
      }

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
