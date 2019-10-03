package shop.http.routes.secured

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.http.auth.roles.CommonUser
import shop.http.json._
import shop.http.json.protocol._
import shop.domain.cart._
import shop.domain.item._
import shop.services.ShoppingCart

final case class CartRoutes[F[_]: Sync](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/cart"

  private def getCartId(user: CommonUser): CartId =
    user.value.id.value.coerce[CartId]

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    // Get shopping cart
    case GET -> Root as user =>
      shoppingCart.get(getCartId(user)).flatMap(Ok(_))

    // Add items to the cart
    case ar @ POST -> Root as user =>
      ar.req.decode[Cart] { cart =>
        cart.items
          .map {
            case (k, v) =>
              // TODO: Lookup item by id in database
              val item = Item(k, ItemName("foo"), ItemDescription("bar"), USD(100))
              shoppingCart.add(getCartId(user), item, v)
          }
          .toList
          .sequence *> Created()
      }

    // Modify items in the cart
    case ar @ PUT -> Root as user =>
      ar.req.decode[Cart] { cart =>
        shoppingCart.update(getCartId(user), cart) *> Ok()
      }

    // Remove item from the cart
    case DELETE -> Root / UUIDVar(uuid) as user =>
      shoppingCart.remove(getCartId(user), uuid.coerce[ItemId]) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
