package shop.http.routes.secured

import shop.domain.brand._
import shop.domain.item._
import shop.generators._
import shop.http.auth.users._
import shop.http.routes.admin._
import shop.services.{ Brands, Items }

import cats.data.Kleisli
import cats.effect._
import io.circe.JsonObject
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import suite.HttpSuite

object AdminRoutesSuite extends HttpSuite {

  def authMiddleware(authUser: AdminUser): AuthMiddleware[IO, AdminUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  test("POST create brand") {
    val gen = for {
      i <- brandIdGen
      u <- adminUserGen
      b <- brandParamGen
    } yield (i, u, b)

    forall(gen) {
      case (id, user, brand) =>
        val req      = POST(brand, uri"/brands")
        val routes   = AdminBrandRoutes[IO](new TestBrands(id)).routes(authMiddleware(user))
        val expected = JsonObject.singleton("brand_id", id.asJson).asJson
        expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }

  test("POST create item") {
    val gen = for {
      i <- itemIdGen
      u <- adminUserGen
      c <- createItemParamGen
    } yield (i, u, c)

    forall(gen) {
      case (id, user, item) =>
        val req      = POST(item, uri"/items")
        val routes   = AdminItemRoutes[IO](new TestItems(id)).routes(authMiddleware(user))
        val expected = JsonObject.singleton("item_id", id.asJson).asJson
        expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }

}

protected class TestBrands(id: BrandId) extends Brands[IO] {
  def findAll: IO[List[Brand]]             = ???
  def create(name: BrandName): IO[BrandId] = IO.pure(id)
}

protected class TestItems(id: ItemId) extends Items[IO] {
  def findAll: IO[List[Item]]                    = ???
  def findBy(brand: BrandName): IO[List[Item]]   = ???
  def findById(itemId: ItemId): IO[Option[Item]] = ???
  def create(item: CreateItem): IO[ItemId]       = IO.pure(id)
  def update(item: UpdateItem): IO[Unit]         = IO.unit
}
