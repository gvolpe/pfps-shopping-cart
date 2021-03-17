package shop.ext.http4s

import derevo.{ Derivation, NewTypeDerivation }
import io.circe.KeyDecoder
import magnolia.{ CaseClass, Magnolia }

object keyDecoder extends Derivation[KeyDecoder] with NewTypeDerivation[KeyDecoder] {
  type Typeclass[T] = KeyDecoder[T]

  def combine[T](ctx: CaseClass[KeyDecoder, T]): KeyDecoder[T] = new KeyDecoder[T] {
    def apply(key: String): Option[T] =
      ctx.parameters.toList match {
        case (p :: _) => p.typeclass.apply(key).map(_.asInstanceOf[T])
        case _        => None
      }
  }

  def instance[T]: KeyDecoder[T] = macro Magnolia.gen[T]
}
