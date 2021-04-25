package shop.ext.http4s

import scala.annotation.implicitNotFound

import derevo.{ Derivation, NewTypeDerivation }
import org.http4s.QueryParamDecoder

object queryParam extends Derivation[QueryParamDecoder] with NewTypeDerivation[QueryParamDecoder] {
  def instance(implicit ev: OnlyNewtypes): Nothing = ev.absurd

  @implicitNotFound("use @derive(queryParam) only for newtypes")
  abstract final class OnlyNewtypes {
    def absurd: Nothing = ???
  }
}
