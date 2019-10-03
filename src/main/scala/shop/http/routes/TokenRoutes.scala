package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.http.json._
import shop.http.json.protocol._
import shop.services.TokenService

final case class TokenRoutes[F[_]: Sync](
    tokenService: TokenService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/token"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok(tokenService.create)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
