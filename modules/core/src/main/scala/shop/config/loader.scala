package shop.config

import cats.effect._
import cats.implicits._
import ciris._
import ciris.refined._
import environments._
import environments.AppEnvironment._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import scala.concurrent.duration._
import shop.config.data._

object load {
  // Ciris promotes configuration as code
  def apply[F[_]: Async: ContextShift]: F[AppConfig] = {
    val config =
      (
        env("SC_APP_ENV").as[AppEnvironment],
        env("SC_JWT_SECRET_KEY").as[NonEmptyString].secret,
        env("SC_JWT_CLAIM").as[NonEmptyString].secret,
        env("SC_ACCESS_TOKEN_SECRET_KEY").as[NonEmptyString].secret,
        env("SC_ADMIN_USER_TOKEN").as[NonEmptyString].secret,
        env("SC_PASSWORD_SALT").as[NonEmptyString].secret
      ).parMapN { (environment, secretKey, claimStr, tokenKey, adminToken, salt) =>
        AppConfig(
          AdminJwtConfig(
            JwtSecretKeyConfig(secretKey),
            JwtClaimConfig(claimStr),
            AdminUserTokenConfig(adminToken)
          ),
          JwtSecretKeyConfig(tokenKey),
          PasswordSalt(salt),
          TokenExpiration(30.minutes),
          ShoppingCartExpiration(30.minutes),
          CheckoutConfig(
            retriesLimit = 3,
            retriesBackoff = 10.milliseconds
          ),
          PaymentConfig(PaymentURI(environment match {
            case Test => "http://10.123.154.10/api"
            case Prod => "https://payments.net/api"
          })),
          HttpClientConfig(
            connectTimeout = 2.seconds,
            requestTimeout = 2.seconds
          ),
          PostgreSQLConfig(
            host = "localhost",
            port = 5432,
            user = "postgres",
            database = "store",
            max = 10
          ),
          RedisConfig(RedisURI(environment match {
            case Test => "redis://localhost"
            case Prod => "redis://10.123.154.176"
          })),
          HttpServerConfig(
            host = "0.0.0.0",
            port = 8080
          )
        )
      }

    config.load[F]
  }
}
