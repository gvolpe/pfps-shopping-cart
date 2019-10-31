package shop.algebras

import cats.{ Monad, Parallel }
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import scala.concurrent.duration._
import shop.domain.healthcheck._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object LiveHealthCheck {
  def make[F[_]: Concurrent: Parallel: Timer](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] =
    new LiveHealthCheck[F](sessionPool, redis).pure[F].widen
}

final class LiveHealthCheck[F[_]: Concurrent: Parallel: Timer] private (
    sessionPool: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthCheck[F] {

  val q: Query[Void, Int] =
    sql"SELECT pid FROM pg_stat_activity".query(int4)

  val redisHealth: F[Boolean] =
    redis.ping
      .map(_.nonEmpty)
      .timeoutTo(1.second, false.pure[F])

  val postgresHealth: F[Boolean] =
    sessionPool
      .use(_.execute(q))
      .map(_.nonEmpty)
      .timeoutTo(1.second, false.pure[F])

  val status: F[AppStatus] =
    (redisHealth, postgresHealth).parMapN(AppStatus)

}
