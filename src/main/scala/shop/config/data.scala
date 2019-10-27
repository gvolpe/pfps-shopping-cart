package shop.config

import ciris._
import ciris.refined._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.{ PosInt, PosLong }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import scala.concurrent.duration._

object data {

  implicit def coercibleConfigDecoder[A: Coercible[String, ?]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, String].map(_.coerce[A])

  implicit def coercibleNonEmptyStringConfigDecoder[A: Coercible[NonEmptyString, ?]]: ConfigDecoder[String, Secret[A]] =
    ConfigDecoder[String, Secret[NonEmptyString]].map(x => Secret(x.value.coerce[A]))

  @newtype case class AdminUserTokenConfig(value: NonEmptyString)
  @newtype case class JwtSecretKeyConfig(value: NonEmptyString)
  @newtype case class JwtClaimConfig(value: NonEmptyString)
  @newtype case class TokenConfig(secretKey: Secret[JwtSecretKeyConfig])
  @newtype case class TokenExpiration(value: FiniteDuration)

  @newtype case class PasswordSalt(value: NonEmptyString)
  @newtype case class PasswordConfig(secret: Secret[PasswordSalt])

  @newtype case class ShoppingCartExpiration(value: FiniteDuration)

  case class CheckoutConfig(
      retriesLimit: PosInt,
      retriesBackoff: FiniteDuration
  )

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: TokenConfig,
      passwordSalt: PasswordConfig,
      tokenExpiration: TokenExpiration,
      cartExpiration: ShoppingCartExpiration,
      checkoutConfig: CheckoutConfig,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig
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
      database: NonEmptyString,
      max: PosLong
  )

  case class RedisConfig(
      uri: NonEmptyString
  )

}
