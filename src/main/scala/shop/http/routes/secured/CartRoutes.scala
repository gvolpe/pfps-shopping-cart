package shop.http.routes.secured

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.ShoppingCart
import shop.http.auth.roles.CommonUser
import shop.http.json._
import shop.domain.cart._
import shop.domain.item._

final class CartRoutes[F[_]: Sync](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    // Get shopping cart
    case GET -> Root as user =>
      Ok(shoppingCart.get(user.value.id))

    // Add items to the cart
    case ar @ POST -> Root as user =>
      ar.req.decode[Cart] { cart =>
        cart.items
          .map {
            case (id, quantity) =>
              shoppingCart.add(user.value.id, id, quantity)
          }
          .toList
          .sequence *> Created()
      }

    // Modify items in the cart
    case ar @ PUT -> Root as user =>
      ar.req.decode[Cart] { cart =>
        shoppingCart.update(user.value.id, cart) *> Ok()
      }

    // Remove item from the cart
    case DELETE -> Root / UUIDVar(uuid) as user =>
      shoppingCart.removeItem(user.value.id, uuid.coerce[ItemId]) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
