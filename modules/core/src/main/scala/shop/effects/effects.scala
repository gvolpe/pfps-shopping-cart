package shop

import cats.effect._

package object effects {

  object BracketThrow {
    def apply[F[_]: BracketThrow]: BracketThrow[F] = implicitly
  }

  object ApThrow {
    def apply[F[_]: ApplicativeThrow]: ApplicativeThrow[F] = implicitly
  }

  object MonadThrow {
    def apply[F[_]: MonadThrow]: MonadThrow[F] = implicitly
  }

}
