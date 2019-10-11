package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.http.json._
import shop.services.ItemService

final case class ItemRoutes[F[_]: Sync](
    itemService: ItemService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/items"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      Ok(itemService.getAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
