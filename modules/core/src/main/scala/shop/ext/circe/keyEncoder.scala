package shop.ext.circe

import derevo.{ Derivation, NewTypeDerivation }
import io.circe.KeyEncoder
import magnolia.{CaseClass, Magnolia, SealedTrait}

class keyEncoder(sep: String = "::") {
  type Typeclass[T] = KeyEncoder[T]

  def combine[T](ctx: CaseClass[KeyEncoder, T]): KeyEncoder[T] =
    if (ctx.isObject) _ => ctx.typeName.short
    else { cc =>
      ctx.parameters.view.map(p => p.typeclass(p.dereference(cc))).mkString(sep)
    }

  def dispatch[T](ctx: SealedTrait[KeyEncoder, T]): KeyEncoder[T] =
    obj => ctx.dispatch(obj)(sub => sub.typeclass(sub.cast(obj)))

  def instance[T]: KeyEncoder[T] = macro Magnolia.gen[T]
}

object keyEncoder extends keyEncoder("::") with Derivation[KeyEncoder] with NewTypeDerivation[KeyEncoder]
