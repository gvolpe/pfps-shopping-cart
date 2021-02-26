package shop.http.routes

import shop.algebras.Brands
import shop.arbitraries._
import shop.domain.brand._

import cats.effect._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.scalacheck.Prop._
import suite._

class BrandRoutesSuite extends HttpTestSuite {

  def dataBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.pure(brands)
  }

  def failingBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.raiseError(DummyError) *> IO.pure(brands)
  }

  test("GET brands [OK]") {
    forAll { (b: List[Brand]) =>
      IOAssertion {
        GET(Uri.uri("/brands")).flatMap { req =>
          val routes = new BrandRoutes[IO](dataBrands(b)).routes
          assertHttp(routes, req)(Status.Ok, b)
        }
      }
    }
  }

  test("GET brands [ERROR]") {
    forAll { (b: List[Brand]) =>
      IOAssertion {
        GET(Uri.uri("/brands")).flatMap { req =>
          val routes = new BrandRoutes[IO](failingBrands(b)).routes
          assertHttpFailure(routes, req)
        }
      }
    }
  }

}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[Unit] = IO.unit
  def findAll: IO[List[Brand]]          = IO.pure(List.empty)
}
