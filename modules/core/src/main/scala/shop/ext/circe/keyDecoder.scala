package shop.ext.circe

import derevo.{ Derivation, NewTypeDerivation }
import io.circe.KeyDecoder
import magnolia.{ CaseClass, Magnolia }

object keyDecoder extends Derivation[KeyDecoder] with NewTypeDerivation[KeyDecoder] {
  type Typeclass[T] = KeyDecoder[T]

  def combine[T](ctx: CaseClass[KeyDecoder, T]): KeyDecoder[T] = new KeyDecoder[T] {
    def apply(key: String): Option[T] = {
      val parts = key.split("::")
      if (parts.length != ctx.parameters.length) None
      else ctx.constructMonadic(p => p.typeclass.apply(parts(p.index)))
    }
  }

  def instance[T]: KeyDecoder[T] = macro Magnolia.gen[T]
}
