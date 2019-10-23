package shop

import cats.Parallel
import cats.effect._
import cats.implicits._
import config._
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{ RedisClient, RedisURI }
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import natchez.Trace.Implicits.noop // needed for skunk
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import skunk._

case class AppResources[F[_]](
    cfg: AppConfig,
    client: Client[F],
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
)

object AppResources {

  def make[F[_]: ConcurrentEffect: ContextShift: Logger]: Resource[F, AppResources[F]] = {

    def mkPostgreSqlResource(cfg: PostgreSQLConfig): SessionPool[F] =
      Session
        .pooled[F](
          host = cfg.host.value,
          port = cfg.port.value,
          user = cfg.user.value,
          database = cfg.database.value,
          max = cfg.max.value
        )

    def mkRedisResource(cfg: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      for {
        uri <- Resource.liftF(RedisURI.make[F](cfg.uri.value))
        client <- RedisClient[F](uri)
        cmd <- Redis[F, String, String](client, RedisCodec.Utf8, uri)
      } yield cmd

    for {
      cfg <- Resource.liftF(config.load[F])
      _ <- Resource.liftF(Logger[F].info(s"Loaded config $cfg"))
      client <- BlazeClientBuilder[F](ExecutionContext.global).resource
      psql <- mkPostgreSqlResource(cfg.postgreSQL)
      redis <- mkRedisResource(cfg.redis)
    } yield AppResources[F](cfg, client, psql, redis)
  }

}
