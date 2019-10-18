package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.Auth
import shop.domain.auth._
import shop.http.json._
import shop.http.auth.roles._

final class LogoutRoutes[F[_]: Sync](
    auth: Auth[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of {

    case req @ POST -> Root / "logout" =>
      AuthHeaders
        .getBearerToken(req)
        .fold(().pure[F])(auth.logout) *> NoContent()

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
