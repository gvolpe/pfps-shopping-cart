package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import java.{ util => ju }
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._
import skunk._

object Algebras {
  def make[F[_]: Sync](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): F[Algebras[F]] =
    for {
      brands <- LiveBrands.make[F](sessionPool)
      categories <- LiveCategories.make[F](sessionPool)
      items <- LiveItems.make[F](sessionPool)
      cart <- LiveShoppingCart.make[F](items, redis, cartExpiration)
      orders <- LiveOrders.make[F](sessionPool)
    } yield new Algebras[F](cart, brands, categories, items, orders)
}

class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F]
) {}
