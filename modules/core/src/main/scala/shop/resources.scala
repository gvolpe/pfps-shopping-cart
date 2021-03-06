package shop

import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import eu.timepit.refined.auto._
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.Logger
import skunk._

import config.data._

final case class AppResources[F[_]](
    client: Client[F],
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
)

object AppResources {

  def make[F[_]: Concurrent: ContextShift: Logger: Timer](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {

    def mkPostgreSqlResource(c: PostgreSQLConfig): SessionPool[F] =
      Session
        .pooled[F](
          host = c.host.value,
          port = c.port.value,
          user = c.user.value,
          database = c.database.value,
          max = c.max.value
        )

    def mkRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value)

    def mkHttpClient(c: HttpClientConfig): Resource[F, Client[F]] =
      EmberClientBuilder
        .default[F]
        .withTimeout(c.timeout)
        .withIdleTimeInPool(c.idleTimeInPool)
        .build

    (
      mkHttpClient(cfg.httpClientConfig),
      mkPostgreSqlResource(cfg.postgreSQL),
      mkRedisResource(cfg.redis)
    ).mapN(AppResources.apply[F])

  }

}
