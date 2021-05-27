package shop.http.routes.secured

import shop.domain.cart._
import shop.http.auth.users.CommonUser
import shop.http.vars.ItemIdVar
import shop.services.ShoppingCart

import cats.Monad
import cats.syntax.all._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._

final case class CartRoutes[F[_]: JsonDecoder: Monad](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    // Get shopping cart
    case GET -> Root as user =>
      Ok(shoppingCart.get(user.value.id))

    // Add items to the cart
    case ar @ POST -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap {
        _.items
          .map {
            case (id, quantity) =>
              shoppingCart.add(user.value.id, id, quantity)
          }
          .toList
          .sequence *> Created()
      }

    // Modify items in the cart
    case ar @ PUT -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap { cart =>
        shoppingCart.update(user.value.id, cart) *> Ok()
      }

    // Remove item from the cart
    case DELETE -> Root / ItemIdVar(itemId) as user =>
      shoppingCart.removeItem(user.value.id, itemId) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
