package shop

import cats._
import cats.effect._

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

}
