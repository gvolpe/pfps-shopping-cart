package shop.http.routes

import cats._
import org.http4s._
import org.http4s.dsl.Http4sDsl
//import org.http4s.server.Router
import shop.algebras.Categories
import shop.http.json._
import shop.http.HttpRouter

final class CategoryRoutes[F[_]: Defer: Monad](
    categories: Categories[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      Ok(categories.findAll)
  }

  val routes: HttpRoutes[F] = HttpRouter(
    prefixPath -> httpRoutes
  )

}
