package shop.modules

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.estatico.newtype.ops._
import java.{ util => ju }
import pdi.jwt._
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._

object Security {
  def make[F[_]: Sync](
      jwtConfig: JwtConfig,
      tokenConfig: TokenConfig
  ): F[Security[F]] = {

    val adminJwtAuth: AdminJwtAuth = JwtAuth(
      JwtSecretKey(jwtConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[AdminJwtAuth]

    val userJwtAuth: UserJwtAuth = JwtAuth(
      JwtSecretKey(tokenConfig.secretKey.value.value),
      JwtAlgorithm.HS256
    ).coerce[UserJwtAuth]

    // FIXME: Hardcoded Admin stuff and side-effectful
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
      tokens <- LiveTokens.make[F](tokenConfig)
      auth <- LiveAuth.make[F](adminToken, adminUser, adminJwtAuth, userJwtAuth, tokens)
    } yield new Security[F](auth)

  }
}

class Security[F[_]] private (
    val auth: Auth[F]
) {}
