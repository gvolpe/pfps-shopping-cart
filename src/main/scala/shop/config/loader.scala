package shop.config

import cats.effect.Sync
import cats.mtl.ApplicativeAsk
import cats.implicits._
import ciris._
import ciris.cats.effect._
import ciris.enumeratum._
import ciris.refined._
import environments._
import environments.AppEnvironment._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.ops._
import scala.concurrent.duration._
import shop.config.data._

object load {

  type HasAppConfig[F[_]] = ApplicativeAsk[F, AppConfig]

  // Ciris promotes configuration as code
  def apply[F[_]: Sync]: F[AppConfig] =
    withValue(envF[F, AppEnvironment]("SC_APP_ENV")) {
      case Test => default[F](redisUri = "redis://localhost")
      case Prod => default[F](redisUri = "redis://10.123.154.176")
    }.orRaiseThrowable

  private def default[F[_]: Sync](redisUri: NonEmptyString) =
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
        salt.coerce[PasswordConfig],
        30.minutes.coerce[TokenExpiration],
        30.minutes.coerce[ShoppingCartExpiration],
        CheckoutConfig(
          retriesLimit = 3,
          retriesBackoff = 10.milliseconds
        ),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "store",
          max = 10L
        ),
        RedisConfig(redisUri)
      )
    }

}
