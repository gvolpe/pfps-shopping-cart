package shop.modules

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import shop.config.TokenConfig
import shop.services._

object Services {
  def make[F[_]: GenUUID: Sync](tokenConfig: TokenConfig): F[Services[F]] =
    for {
      cart <- LiveShoppingCart.make[F]
      token <- LiveTokenService.make[F](tokenConfig)
      brand <- LiveBrandService.make[F]
      category <- LiveCategoryService.make[F]
      item <- LiveItemService.make[F]
      auth <- LiveAuthService.make[F]
    } yield new Services[F](cart, brand, category, item, token, auth)
}

class Services[F[_]: Applicative: GenUUID] private (
    val cart: ShoppingCart[F],
    val brand: BrandService[F],
    val category: CategoryService[F],
    val item: ItemService[F],
    val token: TokenService[F],
    val auth: AuthService[F]
) {}
