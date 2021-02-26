package shop.http.routes

import shop.algebras.Items
import shop.domain.brand._
import shop.domain.item._
import shop.generators._

import cats.effect._
import cats.syntax.option._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.scalacheck.Gen
import suite.HttpSuite

object ItemRoutesSuite extends HttpSuite {

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

  test("GET items succeeds") {
    forall(Gen.listOf(itemGen)) { it =>
      GET(Uri.uri("/items")).flatMap { req =>
        val routes = new ItemRoutes[IO](dataItems(it)).routes
        assertHttp(routes, req)(Status.Ok, it)
      }
    }
  }

  test("GET items by brand succeeds") {
    val gen = for {
      i <- Gen.listOf(itemGen)
      b <- brandGen
    } yield i -> b

    forall(gen) {
      case (it, b) =>
        GET(Uri.uri("/items").withQueryParam(b.name.value)).flatMap { req =>
          val routes = new ItemRoutes[IO](dataItems(it)).routes
          assertHttp(routes, req)(Status.Ok, it)
        }
    }
  }

  test("GET items fails") {
    forall(Gen.listOf(itemGen)) { it =>
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
