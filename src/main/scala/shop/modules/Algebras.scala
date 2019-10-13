package shop.modules

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.estatico.newtype.ops._
import java.{ util => ju }
import pdi.jwt._
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._

object Algebras {
  def make[F[_]: GenUUID: Sync](
      jwtConfig: JwtConfig,
      tokenConfig: TokenConfig
  ): F[Algebras[F]] = {
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
      token <- LiveTokens.make[F](tokenConfig)
      brand <- LiveBrands.make[F]
      category <- LiveCategories.make[F]
      item <- LiveItems.make[F]
      auth <- LiveAuth.make[F](adminToken, adminUser, adminJwtAuth, userJwtAuth, token)
      orders <- LiveOrders.make[F]
    } yield new Algebras[F](cart, brand, category, item, token, auth, orders)
  }
}

class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val tokens: Tokens[F],
    val auth: Auth[F],
    val orders: Orders[F]
) {}
