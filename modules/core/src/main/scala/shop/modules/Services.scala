package shop.modules

import shop.config.data._
import shop.services._

import cats.Parallel
import cats.effect._
import dev.profunktor.redis4cats.RedisCommands
import skunk._
import cats.effect.Temporal

object Services {
  def make[F[_]: Concurrent: Parallel: Temporal](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): Services[F] = {
    val _items = Items.make[F](sessionPool)
    Services[F](
      cart = ShoppingCart.make[F](_items, redis, cartExpiration),
      brands = Brands.make[F](sessionPool),
      categories = Categories.make[F](sessionPool),
      items = _items,
      orders = Orders.make[F](sessionPool),
      healthCheck = HealthCheck.make[F](sessionPool, redis)
    )
  }
}

final case class Services[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F],
    val healthCheck: HealthCheck[F]
)
