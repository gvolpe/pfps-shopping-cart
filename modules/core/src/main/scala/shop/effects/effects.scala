package shop

import cats._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.mtl._
import shop.config.data.AppConfig

package object effects {

  type BracketThrow[F[_]] = Bracket[F, Throwable]

  object BracketThrow {
    def apply[F[_]](implicit ev: Bracket[F, Throwable]): BracketThrow[F] = ev
  }

  type ApThrow[F[_]] = ApplicativeError[F, Throwable]

  object ApThrow {
    def apply[F[_]](implicit ev: ApplicativeError[F, Throwable]): ApThrow[F] = ev
  }

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

  // Not the most correct but okay for performance boost
  implicit def ioAppConfigAsk(implicit cs: ContextShift[IO]): ApplicativeAsk[IO, AppConfig] =
    new DefaultApplicativeAsk[IO, AppConfig] {
      val ref = Ref.unsafe[IO, Option[AppConfig]](None)

      val applicative: Applicative[IO] = implicitly

      def ask: IO[AppConfig] = ref.get.flatMap {
        case Some(cfg) => IO.pure(cfg)
        case None =>
          config.load.apply[IO].flatTap { cfg =>
            ref.set(cfg.some)
          }
      }

      override def reader[A](f: AppConfig => A): IO[A] =
        ask.map(f)
    }

}
