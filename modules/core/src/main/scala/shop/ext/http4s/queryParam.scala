package shop.ext.http4s

import cats.data.{ Validated, ValidatedNel }
import derevo.{ Derivation, NewTypeDerivation }
import magnolia.{ CaseClass, Magnolia }
import org.http4s._

object queryParam extends Derivation[QueryParamDecoder] with NewTypeDerivation[QueryParamDecoder] {
  type Typeclass[T] = QueryParamDecoder[T]

  def combine[T](ctx: CaseClass[QueryParamDecoder, T]): QueryParamDecoder[T] = new QueryParamDecoder[T] {
    def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, T] =
      ctx.parameters.toList match {
        case (p :: _) => p.typeclass.decode(value).map(_.asInstanceOf[T])
        case _        => Validated.invalidNel(ParseFailure("error", "invalid"))
      }
  }

  def instance[T]: QueryParamDecoder[T] = macro Magnolia.gen[T]
}
