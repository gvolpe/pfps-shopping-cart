package shop

import cats.effect._

package object effects {

  object BracketThrow {
    def apply[F[_]](implicit ev: BracketThrow[F]): BracketThrow[F] = ev
  }

  object ApThrow {
    def apply[F[_]](implicit ev: ApplicativeThrow[F]): ApplicativeThrow[F] = ev
  }

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadThrow[F]): MonadThrow[F] = ev
  }

}
