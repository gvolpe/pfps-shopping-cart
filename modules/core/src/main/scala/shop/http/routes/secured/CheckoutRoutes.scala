package shop.http.routes.secured

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.http.auth.roles.CommonUser
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.order._
import shop.http.decoder._
import shop.http.json._
import shop.programs.CheckoutProgram

final class CheckoutRoutes[F[_]: Sync](
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
