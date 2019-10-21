package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.estatico.newtype.ops._
import java.{ util => ju }
import pdi.jwt._
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._
import skunk.Session

object Security {
  def make[F[_]: Sync](
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: TokenConfig,
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val adminJwtAuth: AdminJwtAuth = JwtAuth(
      JwtSecretKey(adminJwtConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[AdminJwtAuth]

    val userJwtAuth: UserJwtAuth = JwtAuth(
      JwtSecretKey(tokenConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[UserJwtAuth]

    val decodeAdminToken: F[JwtClaim] =
      Jwt
        .decode(
          adminJwtConfig.adminToken.value.value,
          adminJwtAuth.value.secretKey.value,
          Seq(JwtAlgorithm.HS256)
        )
        .liftTo[F]

    val adminToken = JwtToken(adminJwtConfig.adminToken.value.value)

    for {
      adminClaim <- decodeAdminToken
      content = adminClaim.content.replace("{", "0").replace("}", "c")
      adminId <- Sync[F].delay(ju.UUID.fromString(content).coerce[UserId])
      adminUser = User(adminId, "admin".coerce[UserName]).coerce[AdminUser]
      tokens <- LiveTokens.make[F](tokenConfig)
      users <- LiveUsers.make[F](sessionPool)
      auth <- LiveAuth.make[F](adminToken, adminUser, adminJwtAuth, userJwtAuth, tokens, users, redis)
    } yield new Security[F](auth)

  }
}

class Security[F[_]] private (
    val auth: Auth[F]
) {}
