package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.domain.auth._
import shop.http.json._
import shop.services.AuthService
import shop.http.auth.roles._

final case class LogoutRoutes[F[_]: Sync](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case req @ POST -> Root / "logout" as _ =>
      AuthHeaders
        .getBearerToken(req.req)
        .fold(().pure[F])(authService.logout) *> NoContent()

  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
