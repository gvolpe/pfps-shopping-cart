package shop

import cats.MonadError
import cats.mtl.ApplicativeAsk

object utils {

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

  type MonadThrow[F[_]] = MonadError[F, Throwable]

}
