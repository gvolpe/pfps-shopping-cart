package shop.modules

import cats.effect._
import cats.implicits._
import java.{ util => ju }
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._
import skunk._

object Algebras {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Algebras[F]] =
    for {
      cart <- LiveShoppingCart.make[F]
      brand <- LiveBrands.make[F](sessionPool)
      category <- LiveCategories.make[F](sessionPool)
      item <- LiveItems.make[F](sessionPool)
      orders <- LiveOrders.make[F](sessionPool)
    } yield new Algebras[F](cart, brand, category, item, orders)
}

class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F]
) {}
