package shop.modules

import shop.auth._
import shop.config.data._
import shop.domain.auth._
import shop.effects._
import shop.http.auth.users._
import shop.services._

import cats.effect._
import cats.syntax.all._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.auto._
import io.circe.parser.{ decode => jsonDecode }
import pdi.jwt._
import skunk.Session

object Security {
  def make[F[_]: Sync](
      cfg: AppConfig,
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(
        JwtAuth
          .hmac(
            cfg.adminJwtConfig.secretKey.secret.value,
            JwtAlgorithm.HS256
          )
      )

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(
        JwtAuth
          .hmac(
            cfg.tokenConfig.secret.value,
            JwtAlgorithm.HS256
          )
      )

    val adminToken = JwtToken(cfg.adminJwtConfig.adminToken.secret.value)

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content <- ApThrow[F].fromEither(jsonDecode[ClaimContent](adminClaim.content))
      adminUser = AdminUser(User(UserId(content.uuid), UserName("admin")))
      tokens <- JwtExpire.make[F].map(Tokens.make[F](_, cfg.tokenConfig, cfg.tokenExpiration))
      crypto <- Crypto.make[F](cfg.passwordSalt)
      users     = Users.make[F](sessionPool, crypto)
      auth      = Auth.make[F](cfg.tokenExpiration, tokens, users, redis, crypto)
      adminAuth = UsersAuth.admin[F](adminToken, adminUser)
      usersAuth = UsersAuth.common[F](redis)
    } yield new Security[F](auth, adminAuth, usersAuth, adminJwtAuth, userJwtAuth)

  }
}

final class Security[F[_]] private (
    val auth: Auth[F],
    val adminAuth: UsersAuth[F, AdminUser],
    val usersAuth: UsersAuth[F, CommonUser],
    val adminJwtAuth: AdminJwtAuth,
    val userJwtAuth: UserJwtAuth
)
