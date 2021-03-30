package shop

import cats.effect._
import cats.{ ApplicativeThrow, MonadThrow }
import cats.effect.MonadCancelThrow

package object effects {

  object BracketThrow {
    def apply[F[_]: MonadCancelThrow]: MonadCancelThrow[F] = implicitly
  }

  object ApThrow {
    def apply[F[_]: ApplicativeThrow]: ApplicativeThrow[F] = implicitly
  }

  object MonadThrow {
    def apply[F[_]: MonadThrow]: MonadThrow[F] = implicitly
  }

}
