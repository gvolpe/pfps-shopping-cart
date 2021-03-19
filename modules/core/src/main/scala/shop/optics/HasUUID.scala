package shop.optics

import java.util.UUID

import shop.effects.GenUUID

import cats.Functor
import cats.syntax.all._
import derevo._

trait IsUUID[A] {
  def _UUID: Iso[A, UUID]

  def uuid[F[_]: Functor: GenUUID]: F[A] =
    GenUUID[F].make.map(_UUID.reverse)

  def read[F[_]: Functor: GenUUID](str: String): F[A] =
    GenUUID[F].read(str).map(_UUID.reverse)
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val _UUID = Iso[UUID, UUID](identity, identity)
  }
}

object uuid extends Derivation[IsUUID] with NewTypeDerivation[IsUUID] {
  def instance[A]: IsUUID[A] = new IsUUID[A] {
    val _UUID: Iso[A, UUID] = Iso[A, UUID](_.asInstanceOf[UUID], _.asInstanceOf[A])
  }
}
