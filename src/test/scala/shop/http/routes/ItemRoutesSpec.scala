package shop.http.routes

import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.estatico.newtype.ops._
import java.util.UUID
import org.http4s._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import shop.suite.HttpTestSuite
import shop.algebras.Items
import shop.arbitraries._
import shop.domain.brand._
import shop.domain.item._
import shop.http.json._

class ItemRoutesSpec extends HttpTestSuite {

  def dataItems(items: List[Item]) = new TestItems {
    override def findAll: IO[List[Item]] =
      IO.pure(items)
  }

  def failingItems(items: List[Item]) = new TestItems {
    override def findAll: IO[List[Item]] =
      IO.raiseError(DummyError) *> IO.pure(items)
    override def findBy(brand: BrandName): IO[List[Item]] =
      findAll
  }

  forAll { (it: List[Item], id: UUID) =>
    spec(s"GET items [OK] - $id") {
      GET(Uri.uri("/items")).flatMap { req =>
        val routes = new ItemRoutes[IO](dataItems(it)).routes
        assertHttp(routes, req)(Status.Ok, it.asJson.noSpaces)
      }
    }
  }

  forAll { (it: List[Item], b: Brand, id: UUID) =>
    spec(s"GET items by brand [OK] - $id") {
      GET(Uri.uri("/items").withQueryParam(b.name.value)).flatMap { req =>
        val routes = new ItemRoutes[IO](dataItems(it)).routes
        assertHttp(routes, req)(Status.Ok, it.asJson.noSpaces)
      }
    }
  }

  forAll { (it: List[Item], id: UUID) =>
    spec(s"GET items [ERROR] - $id") {
      GET(Uri.uri("/items")).flatMap { req =>
        val routes = new ItemRoutes[IO](failingItems(it)).routes
        assertHttpFailure(routes, req)
      }
    }
  }

}

protected class TestItems extends Items[IO] {
  def findAll: IO[List[Item]]                    = IO.pure(List.empty)
  def findBy(brand: BrandName): IO[List[Item]]   = IO.pure(List.empty)
  def findById(itemId: ItemId): IO[Option[Item]] = IO.pure(none[Item])
  def create(item: CreateItem): IO[Unit]         = IO.unit
  def update(item: UpdateItem): IO[Unit]         = IO.unit
}
