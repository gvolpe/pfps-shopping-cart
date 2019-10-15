package shop

import cats.{ ApplicativeError, MonadError }
import cats.mtl.ApplicativeAsk

package object effects {

  type ApThrow[F[_]]    = ApplicativeError[F, Throwable]
  type MonadThrow[F[_]] = MonadError[F, Throwable]

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

}
