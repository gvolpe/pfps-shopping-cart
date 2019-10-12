package shop.http.routes.admin

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.Items
import shop.domain.item._
import shop.http.auth._
import shop.http.auth.roles.AdminUser
import shop.http.json._

final class AdminItemRoutes[F[_]: Sync](
    items: Items[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of {
      case ar @ POST -> Root as _ =>
        ar.req.decode[CreateItem] { item =>
          Created(items.create(item))
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
