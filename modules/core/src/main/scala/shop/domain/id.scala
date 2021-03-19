package shop.domain

import shop.effects.GenUUID
import shop.optics.IsUUID

import cats.Functor

object ID {
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A]              = IsUUID[A].uuid[F]
  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] = IsUUID[A].read[F](str)
}
