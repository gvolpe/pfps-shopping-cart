package shop.modules

import scala.concurrent.duration._

import shop.http.auth.users._
import shop.http.routes._
import shop.http.routes.admin._
import shop.http.routes.secured._

import cats.effect._
import cats.syntax.all._
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import cats.effect.Temporal

object HttpApi {
  def make[F[_]: Concurrent: Temporal](
      services: Services[F],
      programs: Programs[F],
      security: Security[F]
  ): HttpApi[F] =
    HttpApi[F](services, programs, security)
}

final case class HttpApi[F[_]: Concurrent: Temporal] private (
    services: Services[F],
    programs: Programs[F],
    security: Security[F]
) {
  private val adminMiddleware =
    JwtAuthMiddleware[F, AdminUser](security.adminJwtAuth.value, security.adminAuth.findUser)
  private val usersMiddleware =
    JwtAuthMiddleware[F, CommonUser](security.userJwtAuth.value, security.usersAuth.findUser)

  // Auth routes
  private val loginRoutes  = new LoginRoutes[F](security.auth).routes
  private val logoutRoutes = new LogoutRoutes[F](security.auth).routes(usersMiddleware)
  private val userRoutes   = new UserRoutes[F](security.auth).routes

  // Open routes
  private val healthRoutes   = new HealthRoutes[F](services.healthCheck).routes
  private val brandRoutes    = new BrandRoutes[F](services.brands).routes
  private val categoryRoutes = new CategoryRoutes[F](services.categories).routes
  private val itemRoutes     = new ItemRoutes[F](services.items).routes

  // Secured routes
  private val cartRoutes     = new CartRoutes[F](services.cart).routes(usersMiddleware)
  private val checkoutRoutes = new CheckoutRoutes[F](programs.checkout).routes(usersMiddleware)
  private val orderRoutes    = new OrderRoutes[F](services.orders).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes    = new AdminBrandRoutes[F](services.brands).routes(adminMiddleware)
  private val adminCategoryRoutes = new AdminCategoryRoutes[F](services.categories).routes(adminMiddleware)
  private val adminItemRoutes     = new AdminItemRoutes[F](services.items).routes(adminMiddleware)

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
