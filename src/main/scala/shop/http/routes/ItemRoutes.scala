package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.algebras.Items
import shop.domain.brand._
import shop.domain.conversions._
import shop.http.json._
import shop.http.params._

final class ItemRoutes[F[_]: Sync](
    items: Items[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/items"

  object BrandQueryParam extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root =>
      Ok(items.getAll)

    case GET -> Root :? BrandQueryParam(brand) =>
      Ok(brand.fold(items.getAll)(b => items.findBy(b.asBrand)))

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
