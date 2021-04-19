package shop.optics

import java.util.UUID

import shop.effects.GenUUID

import cats.Functor
import cats.syntax.all._
import derevo._
import monocle.Iso

trait IsUUID[A] {
  def _UUID: Iso[UUID, A]

  def make[F[_]: Functor: GenUUID]: F[A] =
    GenUUID[F].make.map(_UUID.get)

  def read[F[_]: Functor: GenUUID](str: String): F[A] =
    GenUUID[F].read(str).map(_UUID.get)
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val _UUID = Iso[UUID, UUID](identity)(identity)
  }
}

object uuid extends Derivation[IsUUID] with NewTypeDerivation[IsUUID] {
  def instance[A]: IsUUID[A] = new IsUUID[A] {
    val _UUID: Iso[UUID, A] = Iso[UUID, A](_.asInstanceOf[A])(_.asInstanceOf[UUID])
  }
}
