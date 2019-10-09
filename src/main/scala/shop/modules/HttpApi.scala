package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{ JwtAuth, JwtSecretKey, JwtToken }
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, CORS, Timeout }
import org.http4s.server.Router
import pdi.jwt._
import scala.concurrent.duration._
import shop.http.auth._
import shop.http.auth.roles._
import shop.http.routes._
import shop.http.routes.admin._
import shop.http.routes.secured._

object HttpApi {
  def make[F[_]: Concurrent: Timer](
      services: Services[F]
  ): F[HttpApi[F]] =
    (services.auth.adminJwtAuth, services.auth.userJwtAuth).mapN {
      case (admin, users) =>
        new HttpApi[F](services, admin, users)
    }
}

class HttpApi[F[_]: Concurrent: Timer] private (
    services: Services[F],
    adminJwtAuth: AdminJwtAuth,
    userJwtAuth: UserJwtAuth
) {
  private val adminAuth: JwtToken => JwtClaim => F[Option[AdminUser]] = t =>
    c => services.auth.findUser[AdminUser](AdminRole)(t)(c)
  private val usersAuth: JwtToken => JwtClaim => F[Option[CommonUser]] = t =>
    c => services.auth.findUser[CommonUser](UserRole)(t)(c)

  private val adminMiddleware = JwtAuthMiddleware[F, AdminUser](adminJwtAuth.value, adminAuth)
  private val usersMiddleware = JwtAuthMiddleware[F, CommonUser](userJwtAuth.value, usersAuth)

  // Auth routes (open)
  private val loginRoutes = LoginRoutes[F](services.auth).routes
  private val userRoutes  = UserRoutes[F](services.auth).routes

  // Open routes
  private val healthRoutes   = HealthRoutes[F].routes
  private val brandRoutes    = BrandRoutes[F](services.brand).routes
  private val categoryRoutes = CategoryRoutes[F](services.category).routes
  private val itemRoutes     = ItemRoutes[F](services.item).routes

  // Secured routes
  private val cartRoutes = CartRoutes[F](services.cart).routes(usersMiddleware)

  // Auth routes (secured)
  private val logoutRoutes = LogoutRoutes[F](services.auth).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes    = AdminBrandRoutes[F](services.brand).routes(adminMiddleware)
  private val adminCategoryRoutes = AdminCategoryRoutes[F](services.category).routes(adminMiddleware)
  private val adminItemRoutes     = AdminItemRoutes[F](services.item).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> itemRoutes <+> brandRoutes <+>
      categoryRoutes <+> loginRoutes <+> userRoutes <+>
      logoutRoutes <+> cartRoutes

  private val adminRoutes: HttpRoutes[F] =
    adminItemRoutes <+> adminBrandRoutes <+> adminCategoryRoutes

  private val routes: HttpRoutes[F] = Router(
    version.v1 -> openRoutes,
    version.v1 + "/admin" -> adminRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  val httpApp: HttpApp[F] = middleware(routes).orNotFound

}
