package shop.modules

import cats.effect.Sync
import cats.implicits._
import java.{ util => ju }
import shop.algebras._
import shop.config._
import shop.domain.auth._
import shop.http.auth.roles._
import skunk._

object Algebras {
  def make[F[_]: Sync](
      session: Session[F]
  ): F[Algebras[F]] =
    for {
      cart <- LiveShoppingCart.make[F]
      brand <- LiveBrands.make[F]
      category <- LiveCategories.make[F]
      item <- LiveItems.make[F](session)
      orders <- LiveOrders.make[F]
    } yield new Algebras[F](cart, brand, category, item, orders)
}

class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val items: Items[F],
    val orders: Orders[F]
) {}
