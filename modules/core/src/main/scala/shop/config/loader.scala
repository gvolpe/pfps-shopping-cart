package shop.config

import scala.concurrent.duration._
import scala.util.control.NoStackTrace

import shop.config.data._

import cats.effect.Async
import cats.syntax.all._
import ciris._
import ciris.refined._
import com.comcast.ip4s.{ Host, Port }
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import environments._
import environments.AppEnvironment._

object load {

  case object InvalidHostOrPort extends NoStackTrace

  // Ciris promotes configuration as code
  def apply[F[_]: Async]: F[AppConfig] =
    (Host.fromString("0.0.0.0"), Port.fromInt(8080)).tupled
      .liftTo[F](InvalidHostOrPort)
      .map { case (h, p) => default[F](h, p) _ }
      .flatMap { mkCfg =>
        env("SC_APP_ENV")
          .as[AppEnvironment]
          .flatMap {
            case Test =>
              mkCfg(
                RedisURI("redis://localhost"),
                PaymentURI("https://payments.free.beeceptor.com")
              )
            case Prod =>
              mkCfg(
                RedisURI("redis://10.123.154.176"),
                PaymentURI("https://payments.net/api")
              )
          }
          .load[F]
      }

  private def default[F[_]](
      host: Host,
      port: Port
  )(
      redisUri: RedisURI,
      paymentUri: PaymentURI
  ): ConfigValue[F, AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[NonEmptyString].secret,
      env("SC_JWT_CLAIM").as[NonEmptyString].secret,
      env("SC_ACCESS_TOKEN_SECRET_KEY").as[NonEmptyString].secret,
      env("SC_ADMIN_USER_TOKEN").as[NonEmptyString].secret,
      env("SC_PASSWORD_SALT").as[NonEmptyString].secret,
      env("SC_POSTGRES_PASSWORD").as[NonEmptyString].secret
    ).parMapN { (secretKey, claimStr, tokenKey, adminToken, salt, postgresPassword) =>
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
        PaymentConfig(paymentUri),
        HttpClientConfig(
          timeout = 60.seconds,
          idleTimeInPool = 30.seconds
        ),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          password = postgresPassword,
          database = "store",
          max = 10
        ),
        RedisConfig(redisUri),
        HttpServerConfig(host, port)
      )
    }

}
