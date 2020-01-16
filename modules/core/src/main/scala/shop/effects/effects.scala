package shop

import cats._
import cats.effect._
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

  // Not the most correct but okay for performance boost
  def makeAskInstance(cfg: AppConfig): ApplicativeAsk[IO, AppConfig] =
    new DefaultApplicativeAsk[IO, AppConfig] {

      val applicative: Applicative[IO] = implicitly

      def ask: IO[AppConfig] = IO.pure(cfg)

      override def reader[A](f: AppConfig => A): IO[A] =
        ask.map(f)
    }

}
