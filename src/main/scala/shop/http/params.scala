package shop.http

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.QueryParamDecoder

object params {

  implicit def coercibleQueryParamDecoder[A: Coercible[B, ?], B: QueryParamDecoder]: QueryParamDecoder[A] =
    QueryParamDecoder[B].map(_.coerce[A])

}
