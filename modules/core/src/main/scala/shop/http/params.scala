package shop.http

import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }
import org.http4s.{ ParseFailure, QueryParamDecoder }

object params {

  implicit def refinedQueryParamDecoder[T: QueryParamDecoder, P](
      implicit ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(refineV[P](_).leftMap(m => ParseFailure(m, m)))

}
