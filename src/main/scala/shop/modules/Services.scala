package shop.modules

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.estatico.newtype.ops._
import java.{ util => ju }
import pdi.jwt._
import shop.config._
import shop.http.auth.roles._
import shop.services._

object Services {
  def make[F[_]: GenUUID: Sync](
      jwtConfig: JwtConfig,
      tokenConfig: TokenConfig
  ): F[Services[F]] = {
    val adminJwtAuth: AdminJwtAuth = JwtAuth(
      JwtSecretKey(jwtConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[AdminJwtAuth]

    val userJwtAuth: UserJwtAuth = JwtAuth(
      JwtSecretKey(tokenConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[UserJwtAuth]

    // Hardcoded Admin stuff for now
    val adminId = ju.UUID.fromString("004b4457-71c3-4439-a1b2-03820263b59c").coerce[UserId]

    val adminToken =
      JwtToken(
        Jwt.encode(
          JwtClaim(adminId.value.toString),
          adminJwtAuth.value.secretKey.value,
          JwtAlgorithm.HS256
        )
      )

    val adminUser = LoggedUser(adminId, "admin".coerce[UserName]).coerce[AdminUser]

    for {
      cart <- LiveShoppingCart.make[F]
      token <- LiveTokenService.make[F](tokenConfig)
      brand <- LiveBrandService.make[F]
      category <- LiveCategoryService.make[F]
      item <- LiveItemService.make[F]
      auth <- LiveAuthService.make[F](adminToken, adminUser, adminJwtAuth, userJwtAuth, token)
    } yield new Services[F](cart, brand, category, item, token, auth)
  }
}

class Services[F[_]: Applicative: GenUUID] private (
    val cart: ShoppingCart[F],
    val brand: BrandService[F],
    val category: CategoryService[F],
    val item: ItemService[F],
    val token: TokenService[F],
    val auth: AuthService[F]
) {}
