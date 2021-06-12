package shop.services

import shop.config.types.ShoppingCartExpiration
import shop.domain._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import shop.effects._

import cats.MonadThrow
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {
  def make[F[_]: GenUUID: MonadThrow](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      exp: ShoppingCartExpiration
  ): ShoppingCart[F] =
    new ShoppingCart[F] {

      def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
        redis.hSet(userId.show, itemId.show, quantity.show) *>
          redis.expire(userId.show, exp.value).void

      def get(userId: UserId): F[CartTotal] =
        redis.hGetAll(userId.show).flatMap {
          _.toList
            .traverseFilter {
              case (k, v) =>
                for {
                  id <- ID.read[F, ItemId](k)
                  qt <- MonadThrow[F].catchNonFatal(Quantity(v.toInt))
                  rs <- items.findById(id).map(_.map(_.cart(qt)))
                } yield rs
            }
            .map { items =>
              CartTotal(items, items.foldMap(_.subTotal))
            }
        }

      def delete(userId: UserId): F[Unit] =
        redis.del(userId.show).void

      def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
        redis.hDel(userId.show, itemId.show).void

      def update(userId: UserId, cart: Cart): F[Unit] =
        redis.hGetAll(userId.show).flatMap {
          _.toList.traverse_ {
            case (k, _) =>
              ID.read[F, ItemId](k).flatMap { id =>
                cart.items.get(id).traverse_ { q =>
                  redis.hSet(userId.show, k, q.show)
                }
              }
          } *>
            redis.expire(userId.show, exp.value).void
        }
    }
}
