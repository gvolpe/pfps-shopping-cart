package shop.http

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s._

object params {

  implicit def coercibleQueryParamDecoder[A: Coercible[B, ?], B: QueryParamDecoder]: QueryParamDecoder[A] =
    QueryParamDecoder[B].map(_.coerce[A])

  implicit val nonEmptyStringParamDecoder: QueryParamDecoder[String Refined NonEmpty] =
    QueryParamDecoder[String].emap(refineV[NonEmpty](_).leftMap(m => ParseFailure(m, m)))

}
