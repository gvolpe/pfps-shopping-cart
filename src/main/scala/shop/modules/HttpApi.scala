package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{ JwtAuth, JwtSecretKey, JwtToken }
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware._
import org.http4s.server.Router
import pdi.jwt._
import scala.concurrent.duration._
import shop.algebras.Auth
import shop.http.auth._
import shop.http.auth.roles._
import shop.http.routes._
import shop.http.routes.admin._
import shop.http.routes.secured._

object HttpApi {
  def make[F[_]: Concurrent: Timer](
      algebras: Algebras[F],
      programs: Programs[F],
      security: Security[F]
  ): F[HttpApi[F]] =
    (security.auth.adminJwtAuth, security.auth.userJwtAuth).mapN {
      case (admin, users) =>
        new HttpApi[F](security.auth, algebras, programs, admin, users)
    }
}

class HttpApi[F[_]: Concurrent: Timer] private (
    auth: Auth[F],
    algebras: Algebras[F],
    programs: Programs[F],
    adminJwtAuth: AdminJwtAuth,
    userJwtAuth: UserJwtAuth
) {
  private val adminAuth: JwtToken => JwtClaim => F[Option[AdminUser]] = t =>
    c => auth.findUser[AdminUser](AdminRole)(t)(c)
  private val usersAuth: JwtToken => JwtClaim => F[Option[CommonUser]] = t =>
    c => auth.findUser[CommonUser](UserRole)(t)(c)

  private val adminMiddleware = JwtAuthMiddleware[F, AdminUser](adminJwtAuth.value, adminAuth)
  private val usersMiddleware = JwtAuthMiddleware[F, CommonUser](userJwtAuth.value, usersAuth)

  // Auth routes (open)
  private val loginRoutes  = new LoginRoutes[F](auth).routes
  private val logoutRoutes = new LogoutRoutes[F](auth).routes
  private val userRoutes   = new UserRoutes[F](auth).routes

  // Open routes
  private val healthRoutes   = new HealthRoutes[F].routes
  private val brandRoutes    = new BrandRoutes[F](algebras.brands).routes
  private val categoryRoutes = new CategoryRoutes[F](algebras.categories).routes
  private val itemRoutes     = new ItemRoutes[F](algebras.items).routes

  // Secured routes
  private val cartRoutes     = new CartRoutes[F](algebras.cart).routes(usersMiddleware)
  private val checkoutRoutes = new CheckoutRoutes[F](programs.checkout).routes(usersMiddleware)
  private val orderRoutes    = new OrderRoutes[F](algebras.orders).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes    = new AdminBrandRoutes[F](algebras.brands).routes(adminMiddleware)
  private val adminCategoryRoutes = new AdminCategoryRoutes[F](algebras.categories).routes(adminMiddleware)
  private val adminItemRoutes     = new AdminItemRoutes[F](algebras.items).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> itemRoutes <+> brandRoutes <+>
      categoryRoutes <+> loginRoutes <+> userRoutes <+>
      logoutRoutes <+> cartRoutes <+> orderRoutes <+>
      checkoutRoutes

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

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)

}
