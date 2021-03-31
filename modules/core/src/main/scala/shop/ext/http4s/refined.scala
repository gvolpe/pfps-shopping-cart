package shop.ext.http4s

import cats.MonadThrow
import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }
import io.circe.Decoder
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object refined {

  implicit def refinedQueryParamDecoder[T: QueryParamDecoder, P](
      implicit ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(refineV[P](_).leftMap(m => ParseFailure(m, m)))

  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow](req: Request[F]) extends Http4sDsl[F] {

    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      req.asJsonDecode[A].attempt.flatMap {
        case Left(e) =>
          Option(e.getCause) match {
            case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
            case _                                               => UnprocessableEntity()
          }
        case Right(a) => f(a)
      }

  }

}
