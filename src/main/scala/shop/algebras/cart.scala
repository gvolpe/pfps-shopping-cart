package shop.algebras

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import LiveShoppingCart._

trait ShoppingCart[F[_]] {
  def add(cartId: CartId, item: Item, quantity: Quantity): F[Unit]
  def get(cartId: CartId): F[List[CartItem]]
  def findBy(userId: UserId): F[Cart]
  def remove(cartId: CartId, itemId: ItemId): F[Unit]
  def update(cartId: CartId, cart: Cart): F[Unit]
}

object LiveShoppingCart {
  type ItemsInCart = Map[ItemId, CartItem]
  type Carts[F[_]] = Map[CartId, Ref[F, ItemsInCart]]

  def make[F[_]: Sync]: F[ShoppingCart[F]] =
    Ref.of[F, Carts[F]](Map.empty).map(new LiveShoppingCart(_))
}

class LiveShoppingCart[F[_]: Sync] private (
    ref: Ref[F, Carts[F]]
) extends ShoppingCart[F] {

  private val unit = ().pure[F]

  private def createNewCart(cartId: CartId): F[Ref[F, ItemsInCart]] =
    Ref.of[F, ItemsInCart](Map.empty).flatMap { newCart =>
      ref.update(_.updated(cartId, newCart)).as(newCart)
    }

  private def getOrCreateCart(cartId: CartId): F[Ref[F, ItemsInCart]] =
    ref.get.flatMap(_.get(cartId).fold(createNewCart(cartId))(_.pure[F]))

  def add(cartId: CartId, item: Item, quantity: Quantity): F[Unit] =
    getOrCreateCart(cartId).flatMap { cart =>
      cart.update(_.updated(item.uuid, CartItem(item, quantity)))
    }

  def get(cartId: CartId): F[List[CartItem]] =
    ref.get.flatMap { carts =>
      carts.get(cartId).fold(List.empty[CartItem].pure[F]) { cart =>
        cart.get.map(_.values.toList)
      }
    }

  def findBy(userId: UserId): F[Cart] = Cart(Map.empty).pure[F]

  def remove(cartId: CartId, itemId: ItemId): F[Unit] =
    ref.get.flatMap { carts =>
      carts.get(cartId).fold(unit) { cart =>
        cart.update(_.removed(itemId))
      }
    }

  def update(cartId: CartId, cart: Cart): F[Unit] =
    ref.get.flatMap { carts =>
      carts.get(cartId).fold(unit) { st =>
        cart.items
          .map {
            case (id, q) =>
              st.update(_.updatedWith(id)(_.map(_.copy(quantity = q))))
          }
          .toList
          .sequence_
      }
    }
}
