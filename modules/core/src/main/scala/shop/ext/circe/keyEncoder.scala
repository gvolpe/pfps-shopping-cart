package shop.ext.circe

import derevo.{ Derivation, NewTypeDerivation }
import io.circe.KeyEncoder
import magnolia.{ CaseClass, Magnolia }

object keyEncoder extends Derivation[KeyEncoder] with NewTypeDerivation[KeyEncoder] {
  type Typeclass[T] = KeyEncoder[T]

  def combine[T](ctx: CaseClass[KeyEncoder, T]): KeyEncoder[T] = new KeyEncoder[T] {
    def apply(key: T): String =
      ctx.parameters.toList match {
        case (p :: _) => p.typeclass.apply(key.asInstanceOf[p.PType])
        case _        => "error"
      }
  }

  def instance[T]: KeyEncoder[T] = macro Magnolia.gen[T]
}
