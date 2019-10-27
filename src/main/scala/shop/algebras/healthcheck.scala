package shop.algebras

import cats.Monad
import cats.effect._
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import shop.domain.healthcheck._
import shop.effects.BracketThrow
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object LiveHealthCheck {
  def make[F[_]: BracketThrow](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] =
    new LiveHealthCheck[F](sessionPool, redis).pure[F].widen
}

class LiveHealthCheck[F[_]: BracketThrow] private (
    sessionPool: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthCheck[F] {

  val q: Query[Void, Int] =
    sql"SELECT pid FROM pg_stat_activity".query(int4)

  def status: F[AppStatus] =
    for {
      r <- redis.ping.map(_.nonEmpty)
      p <- sessionPool.use(_.execute(q)).map(_.nonEmpty)
    } yield AppStatus(r, p)

}
