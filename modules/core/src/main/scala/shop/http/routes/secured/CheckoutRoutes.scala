package shop.http.routes.secured

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.http.auth.users.CommonUser
import shop.domain.checkout._
import shop.domain.errors._
import shop.http.decoder._
import shop.http.errors._
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
      }

  }

  def routes(
      authMiddleware: AuthMiddleware[F, CommonUser]
  )(implicit H: HttpErrorHandler[F, CheckoutError]): HttpRoutes[F] =
    Router(
      prefixPath -> H.handle(authMiddleware(httpRoutes))
    )

}
