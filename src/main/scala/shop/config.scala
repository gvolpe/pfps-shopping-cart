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

  @newtype case class JwtSecretKeyConfig(value: String)
  @newtype case class JwtClaimConfig(value: String)
  @newtype case class TokenConfig(secretKey: Secret[JwtSecretKeyConfig])

  case class AppConfig(adminJwtConfig: JwtConfig, tokenConfig: TokenConfig)

  case class JwtConfig(secretKey: Secret[JwtSecretKeyConfig], claimStr: Secret[JwtClaimConfig])

  type HasAppConfig[F[_]] = ApplicativeAsk[F, AppConfig]

  implicit def coercibleConfigDecoder[A: Coercible[String, ?]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, String].map(_.coerce[A])

  def load[F[_]: Sync]: F[AppConfig] =
    loadConfig(
      envF[F, Secret[JwtSecretKeyConfig]]("JWT_SECRET_KEY"),
      envF[F, Secret[JwtClaimConfig]]("JWT_CLAIM"),
      envF[F, Secret[JwtSecretKeyConfig]]("ACCESS_TOKEN_SECRET_KEY")
    ) { (secretKey, claimStr, tokenKey) =>
      AppConfig(
        JwtConfig(secretKey, claimStr),
        tokenKey.coerce[TokenConfig]
      )
    }.orRaiseThrowable

}
