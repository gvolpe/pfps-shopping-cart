package shop.optics

import java.util.UUID

import scala.annotation.implicitNotFound

import derevo._
import monocle.Iso

trait IsUUID[A] {
  def _UUID: Iso[UUID, A]
}

object IsUUID {
  def apply[A: IsUUID]: IsUUID[A] = implicitly

  implicit val identityUUID: IsUUID[UUID] = new IsUUID[UUID] {
    val _UUID = Iso[UUID, UUID](identity)(identity)
  }
}

object uuid extends Derivation[IsUUID] with NewTypeDerivation[IsUUID] {
  def instance(implicit ev: OnlyNewtypes): Nothing = ev.absurd

  @implicitNotFound("use @derive(uuid) only for newtypes")
  abstract final class OnlyNewtypes {
    def absurd: Nothing = ???
  }
}
