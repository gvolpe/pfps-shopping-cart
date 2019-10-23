package shop

import cats.effect.Sync
import cats.mtl.ApplicativeAsk
import cats.implicits._
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object config {
  import ciris._, ciris.cats.effect._, ciris.enumeratum._
  import environments._, environments.AppEnvironment._

  @newtype case class AdminUserTokenConfig(value: String)
  @newtype case class JwtSecretKeyConfig(value: String)
  @newtype case class JwtClaimConfig(value: String)
  @newtype case class TokenConfig(secretKey: Secret[JwtSecretKeyConfig])

  @newtype case class PasswordSalt(value: String)
  @newtype case class PasswordConfig(secret: Secret[PasswordSalt])

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: TokenConfig,
      passwordSalt: PasswordConfig,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig
  )

  case class AdminJwtConfig(
      secretKey: Secret[JwtSecretKeyConfig],
      claimStr: Secret[JwtClaimConfig],
      adminToken: Secret[AdminUserTokenConfig]
  )

  // TODO: User ciris-refined
  case class PostgreSQLConfig(
      host: String,
      port: Int,
      user: String,
      database: String,
      max: Long
  )

  case class RedisConfig(
      uri: String
  )

  type HasAppConfig[F[_]] = ApplicativeAsk[F, AppConfig]

  implicit def coercibleConfigDecoder[A: Coercible[String, ?]]: ConfigDecoder[String, A] =
    ConfigDecoder[String, String].map(_.coerce[A])

  // Ciris promotes configuration as code
  def load[F[_]: Sync]: F[AppConfig] =
    withValue(envF[F, AppEnvironment]("SC_APP_ENV")) {
      case Test => default[F](redisUri = "redis://localhost")
      case Prod => default[F](redisUri = "redis://10.123.154.176")
    }.orRaiseThrowable

  private def default[F[_]: Sync](redisUri: String) =
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
