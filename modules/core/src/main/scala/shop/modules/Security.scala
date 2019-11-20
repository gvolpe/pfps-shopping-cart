package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.estatico.newtype.ops._
import pdi.jwt._
import shop.algebras._
import shop.config.data._
import shop.domain.auth._
import shop.http.auth.roles._
import skunk.Session

object Security {
  def make[F[_]: GenUUID: Sync](
      cfg: AppConfig,
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val adminJwtAuth: AdminJwtAuth =
      JwtAuth
        .hmac(
          cfg.adminJwtConfig.secretKey.value.value.value,
          JwtAlgorithm.HS256
        )
        .coerce[AdminJwtAuth]

    val userJwtAuth: UserJwtAuth =
      JwtAuth
        .hmac(
          cfg.tokenConfig.value.value.value,
          JwtAlgorithm.HS256
        )
        .coerce[UserJwtAuth]

    val adminToken = JwtToken(cfg.adminJwtConfig.adminToken.value.value.value)

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content = adminClaim.content.replace("{", "0").replace("}", "c")
      adminId <- GenUUID[F].read[UserId](content)
      adminUser = User(adminId, "admin".coerce[UserName]).coerce[AdminUser]
      authData  = AuthData(adminToken, adminUser, cfg.tokenExpiration)
      tokens <- LiveTokens.make[F](cfg.tokenConfig, cfg.tokenExpiration)
      crypto <- LiveCrypto.make[F](cfg.passwordSalt)
      users <- LiveUsers.make[F](sessionPool, crypto)
      auth <- LiveAuth.make[F](authData, tokens, users, redis)
      adminAuth <- LiveUsersAuth.make[F, AdminUser](authData, redis)
      usersAuth <- LiveUsersAuth.make[F, CommonUser](authData, redis)
    } yield new Security[F](auth, adminAuth, usersAuth, adminJwtAuth, userJwtAuth)

  }
}

final class Security[F[_]] private (
    val auth: Auth[F],
    val adminAuth: UsersAuth[F, AdminUser],
    val usersAuth: UsersAuth[F, CommonUser],
    val adminJwtAuth: AdminJwtAuth,
    val userJwtAuth: UserJwtAuth
) {}
