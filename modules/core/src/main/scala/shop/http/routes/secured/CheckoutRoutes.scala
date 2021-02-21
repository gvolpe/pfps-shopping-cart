package shop.http.routes.secured

import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.order._
import shop.http.auth.users.CommonUser
import shop.http.decoder._
import shop.programs.CheckoutProgram

import cats.Defer
import cats.effect.MonadThrow
import cats.syntax.all._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server._

final class CheckoutRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    program: CheckoutProgram[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case ar @ POST -> Root as user =>
      ar.req.decodeR[Card] { card =>
        program
          .checkout(user.value.id, card)
          .flatMap(Created(_))
          .recoverWith {
            case CartNotFound(userId) =>
              NotFound(s"Cart not found for user: ${userId.value}")
            case EmptyCartError =>
              BadRequest("Shopping cart is empty!")
            case PaymentError(cause) =>
              BadRequest(cause)
            case OrderError(cause) =>
              BadRequest(cause)
          }
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
