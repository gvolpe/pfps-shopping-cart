package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.algebras.Auth
import shop.domain.auth._
import shop.http.auth.roles._
import shop.http.json._

final class UserRoutes[F[_]: Sync](
    auth: Auth[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case req @ POST -> Root / "users" =>
      req
        .decode[CreateUser] { user =>
          auth
            .newUser(user.username.toDomain, user.password.toDomain, UserRole)
            .flatMap(Created(_))
            .handleErrorWith {
              case UserNameInUse(u) => Conflict(u.value)
            }
        }

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
