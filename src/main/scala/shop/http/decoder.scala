package shop.http

import cats.Monad
import cats.implicits._
import io.circe.Decoder
import org.http4s._
import org.http4s.dsl.Http4sDsl
import shop.domain.checkout.Card

object decoder {

  implicit class RefinedRequestDecoder[F[_]: Monad](req: Request[F]) extends Http4sDsl[F] {

    def decodeR[A](f: A => F[Response[F]])(implicit ev: EntityDecoder[F, A]): F[Response[F]] =
      ev.decode(req, strict = false).value.flatMap {
        case Left(e) =>
          e.cause match {
            case Some(c) if c.getMessage.startsWith("Predicate failed") => BadRequest(c.getMessage)
            case _                                                      => UnprocessableEntity()
          }
        case Right(a) => f(a)
      }

  }

}

