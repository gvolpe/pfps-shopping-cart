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
import shop.services.{ AuthService, GenUUID }
import shop.http.auth.roles._

final case class UserRoutes[F[_]: GenUUID: Sync](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  // TODO: Register should return access token
  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    // create a new user
    case req @ POST -> Root / "users" =>
      req.decode[CreateUser] { newUser =>
        GenUUID[F].make.flatMap { uuid =>
          // TODO: 409 on email / username conflict
          val user = LoggedUser(uuid.coerce[UserId], newUser.username.value.coerce[UserName])
          authService.newUser(user, UserRole) >> Created(uuid)
        }
      }

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
