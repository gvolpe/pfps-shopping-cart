package shop.algebras

import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import shop.config.data.ShoppingCartExpiration
import shop.effects._
import squants.market._

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object LiveShoppingCart {
  def make[F[_]: Sync](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      exp: ShoppingCartExpiration
  ): F[ShoppingCart[F]] =
    Sync[F].delay(
      new LiveShoppingCart(items, redis, exp)
    )
}

final class LiveShoppingCart[F[_]: GenUUID: MonadThrow] private (
    items: Items[F],
    redis: RedisCommands[F, String, String],
    exp: ShoppingCartExpiration
) extends ShoppingCart[F] {

  private def calcTotal(items: List[CartItem]): Money =
    USD(
      items
        .foldMap { i =>
          i.item.price.value * i.quantity.value
        }
    )

  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
    redis.hSet(userId.value.toString, itemId.value.toString, quantity.value.toString) *>
        redis.expire(userId.value.toString, exp.value)

  def get(userId: UserId): F[CartTotal] =
    redis.hGetAll(userId.value.toString).flatMap { it =>
      it.toList
        .traverseFilter {
          case (k, v) =>
            for {
              id <- GenUUID[F].read[ItemId](k)
              qt <- ApThrow[F].catchNonFatal(Quantity(v.toInt))
              rs <- items.findById(id).map(_.map(i => CartItem(i, qt)))
            } yield rs
        }
        .map(items => CartTotal(items, calcTotal(items)))
    }

  def delete(userId: UserId): F[Unit] =
    redis.del(userId.value.toString)

  def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.value.toString, itemId.value.toString)

  def update(userId: UserId, cart: Cart): F[Unit] =
    redis.hGetAll(userId.value.toString).flatMap { it =>
      it.toList.traverse_ {
        case (k, _) =>
          GenUUID[F].read[ItemId](k).flatMap { id =>
            cart.items.get(id).traverse_ { q =>
              redis.hSet(userId.value.toString, k, q.value.toString)
            }
          }
      } *>
        redis.expire(userId.value.toString, exp.value)
    }

}
