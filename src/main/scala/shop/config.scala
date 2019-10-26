package shop

import cats.effect.Sync
import cats.mtl.ApplicativeAsk
import cats.implicits._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.{ PosInt, PosLong }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import scala.concurrent.duration._

object config {
  import ciris._, ciris.cats.effect._, ciris.enumeratum._, ciris.refined._
  import environments._, environments.AppEnvironment._

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

  type HasAppConfig[F[_]] = ApplicativeAsk[F, AppConfig]

  implicit def coercibleConfigDecoder[A: Coercible[String, ?]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, String].map(_.coerce[A])

  implicit def coercibleNonEmptyStringConfigDecoder[A: Coercible[NonEmptyString, ?]]: ConfigDecoder[String, Secret[A]] =
    ConfigDecoder[String, Secret[NonEmptyString]].map(x => Secret(x.value.coerce[A]))

  // Ciris promotes configuration as code
  def load[F[_]: Sync]: F[AppConfig] =
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

object environments {
  import enumeratum._
  import enumeratum.EnumEntry._

  sealed abstract class AppEnvironment extends EnumEntry with Lowercase

  object AppEnvironment extends Enum[AppEnvironment] {
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    val values = findValues
  }
}
