package shop

import cats.effect.Sync
import cats.mtl.ApplicativeAsk
import cats.implicits._
import ciris._
import ciris.cats.effect._
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object config {

  @newtype case class AdminUserTokenConfig(value: String)
  @newtype case class JwtSecretKeyConfig(value: String)
  @newtype case class JwtClaimConfig(value: String)
  @newtype case class TokenConfig(secretKey: Secret[JwtSecretKeyConfig])

  @newtype case class PasswordSalt(value: String)
  @newtype case class PasswordConfig(secret: Secret[PasswordSalt])

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: TokenConfig,
      passwordSalt: PasswordConfig
  )

  case class AdminJwtConfig(
      secretKey: Secret[JwtSecretKeyConfig],
      claimStr: Secret[JwtClaimConfig],
      adminToken: Secret[AdminUserTokenConfig]
  )

  type HasAppConfig[F[_]] = ApplicativeAsk[F, AppConfig]

  implicit def coercibleConfigDecoder[A: Coercible[String, ?]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, String].map(_.coerce[A])

  def load[F[_]: Sync]: F[AppConfig] =
    loadConfig(
      envF[F, Secret[JwtSecretKeyConfig]]("SC_JWT_SECRET_KEY"),
      envF[F, Secret[JwtClaimConfig]]("SC_JWT_CLAIM"),
      envF[F, Secret[JwtSecretKeyConfig]]("SC_ACCESS_TOKEN_SECRET_KEY"),
      envF[F, Secret[AdminUserTokenConfig]]("SC_ADMIN_USER_TOKEN"),
      envF[F, Secret[PasswordSalt]]("SC_PASSWORD_SALT")
    ) { (secretKey, claimStr, tokenKey, adminToken, salt) =>
      AppConfig(
        AdminJwtConfig(secretKey, claimStr, adminToken),
        tokenKey.coerce[TokenConfig],
        salt.coerce[PasswordConfig]
      )
    }.orRaiseThrowable

}
