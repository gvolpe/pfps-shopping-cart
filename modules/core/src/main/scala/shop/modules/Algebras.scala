package shop.modules

import shop.algebras._
import shop.config.data._

import cats.Parallel
import cats.effect._
import dev.profunktor.redis4cats.RedisCommands
import skunk._

object Algebras {
  def make[F[_]: Concurrent: Parallel: Timer](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): Algebras[F] = {
    val liveItems = Items.make[F](sessionPool)
    Algebras[F](
      cart = ShoppingCart.make[F](liveItems, redis, cartExpiration),
      brands = Brands.make[F](sessionPool),
      categories = Categories.make[F](sessionPool),
      items = liveItems,
      orders = Orders.make[F](sessionPool),
      healthCheck = HealthCheck.make[F](sessionPool, redis)
    )
  }
}

final case class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F],
    val healthCheck: HealthCheck[F]
)
