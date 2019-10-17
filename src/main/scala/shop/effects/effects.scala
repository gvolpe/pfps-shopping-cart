package shop

import cats.{ ApplicativeError, MonadError }
import cats.mtl.ApplicativeAsk

package object effects {

  type ApThrow[F[_]] = ApplicativeError[F, Throwable]

  object ApThrow {
    def apply[F[_]](implicit ev: ApplicativeError[F, Throwable]): ApThrow[F] = ev
  }

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

}
