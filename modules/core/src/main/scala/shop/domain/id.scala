package shop.domain

import shop.effects.GenUUID
import shop.optics.HasUUID

import cats.Functor

object ID {
  def make[F[_]: Functor: GenUUID, A: HasUUID]: F[A]              = HasUUID[A].uuid[F]
  def read[F[_]: Functor: GenUUID, A: HasUUID](str: String): F[A] = HasUUID[A].read[F](str)
}
