package shop.http.routes.admin

import cats.effect.Sync
import cats.implicits._
import io.circe.refined._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.Brands
import shop.domain.brand._
import shop.http.auth._
import shop.http.auth.roles.AdminUser
import shop.http.decoder._
import shop.http.json._

final class AdminBrandRoutes[F[_]: Sync](
    brands: Brands[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of {
      case ar @ POST -> Root as _ =>
        ar.req.decodeR[BrandParam] { bp =>
          Created(brands.create(bp.toDomain))
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
