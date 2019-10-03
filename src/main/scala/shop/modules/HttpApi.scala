package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{ JwtAuth, JwtSecretKey }
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, CORS, Timeout }
import org.http4s.server.Router
import pdi.jwt._
import scala.concurrent.duration._
import shop.config._
import shop.http.auth._
import shop.http.auth.roles._
import shop.http.routes._
import shop.http.routes.admin._
import shop.http.routes.secured._

object HttpApi {
  def make[F[_]: Concurrent: Timer](
      services: Services[F],
      jwtConfig: JwtConfig,
      tokenConfig: TokenConfig
  ): F[HttpApi[F]] = {
    val adminJwtAuth: AdminJwtAuth = JwtAuth(
      JwtSecretKey(jwtConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[AdminJwtAuth]

    val userJwtAuth: UserJwtAuth = JwtAuth(
      JwtSecretKey(tokenConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[UserJwtAuth]

    new HttpApi[F](services, adminJwtAuth, userJwtAuth).pure[F]
  }
}

class HttpApi[F[_]: Concurrent: Timer] private (
    services: Services[F],
    adminJwtAuth: AdminJwtAuth,
    userJwtAuth: UserJwtAuth
) {

  println(
    Jwt.encode(
      JwtClaim("""{004b4457-71c3-4439-a1b2-03820263b59c}"""),
      adminJwtAuth.value.secretKey.value,
      JwtAlgorithm.HS256
    )
  )

  private val adminAuth = services.auth.findUser[AdminUser](AdminRole)(_)
  private val usersAuth = services.auth.findUser[CommonUser](UserRole)(_)

  private val adminMiddleware = JwtAuthMiddleware[F, AdminUser](adminJwtAuth.value, adminAuth)
  private val usersMiddleware = JwtAuthMiddleware[F, CommonUser](userJwtAuth.value, usersAuth)

  // Open routes
  private val authRoutes     = AuthRoutes[F](services.auth).routes
  private val healthRoutes   = HealthRoutes[F].routes
  private val brandRoutes    = BrandRoutes[F](services.brand).routes
  private val categoryRoutes = CategoryRoutes[F](services.category).routes
  private val itemRoutes     = ItemRoutes[F](services.item).routes
  private val tokenRoutes    = TokenRoutes[F](services.token).routes

  // Secured routes
  private val cartRoutes = CartRoutes[F](services.cart).routes(usersMiddleware)

  // Admin routes
  private val adminBrandRoutes    = AdminBrandRoutes[F](services.brand).routes(adminMiddleware)
  private val adminCategoryRoutes = AdminCategoryRoutes[F](services.category).routes(adminMiddleware)
  private val adminItemRoutes     = AdminItemRoutes[F](services.item).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> authRoutes <+> tokenRoutes <+>
      itemRoutes <+> cartRoutes <+> brandRoutes <+>
      categoryRoutes

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
