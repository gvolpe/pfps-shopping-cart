package shop.config

import scala.concurrent.duration._

import shop.ext.ciris._

import ciris._
import ciris.refined._
import com.comcast.ip4s.{ Host, Port }
import derevo.cats.show
import derevo.derive
import eu.timepit.refined.cats._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object types {

  @derive(configDecoder, show)
  @newtype
  case class AdminUserTokenConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype
  case class JwtSecretKeyConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype
  case class JwtAccessTokenKeyConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype
  case class JwtClaimConfig(secret: NonEmptyString)

  @derive(configDecoder, show)
  @newtype
  case class PasswordSalt(secret: NonEmptyString)

  @newtype case class TokenExpiration(value: FiniteDuration)

  @newtype case class ShoppingCartExpiration(value: FiniteDuration)

  case class CheckoutConfig(
      retriesLimit: PosInt,
      retriesBackoff: FiniteDuration
  )

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: Secret[JwtAccessTokenKeyConfig],
      passwordSalt: Secret[PasswordSalt],
      tokenExpiration: TokenExpiration,
      cartExpiration: ShoppingCartExpiration,
      checkoutConfig: CheckoutConfig,
      paymentConfig: PaymentConfig,
      httpClientConfig: HttpClientConfig,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

  case class AdminJwtConfig(
      secretKey: Secret[JwtSecretKeyConfig],
      claimStr: Secret[JwtClaimConfig],
      adminToken: Secret[AdminUserTokenConfig]
  )

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: UserPortNumber,
      user: NonEmptyString,
      password: Secret[NonEmptyString],
      database: NonEmptyString,
      max: PosInt
  )

  @newtype case class RedisURI(value: NonEmptyString)
  @newtype case class RedisConfig(uri: RedisURI)

  @newtype case class PaymentURI(value: NonEmptyString)
  @newtype case class PaymentConfig(uri: PaymentURI)

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

}
