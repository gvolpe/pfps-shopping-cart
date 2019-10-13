package shop

import cats.MonadError
import cats.mtl.ApplicativeAsk

package object effects {

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  def ask[F[_], A](implicit ev: ApplicativeAsk[F, A]): F[A] = ev.ask

}
