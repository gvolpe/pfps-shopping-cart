package shop.http.routes

import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import shop.algebras.Brands
import shop.arbitraries._
import shop.domain.brand._
import shop.http.json._
import suite.HttpTestSuite

class BrandRoutesSpec extends HttpTestSuite {

  def dataBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.pure(brands)
  }

  def failingBrands(brands: List[Brand]) = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.raiseError(DummyError) *> IO.pure(brands)
  }

  forAll { (b: List[Brand]) =>
    spec("GET brands [OK]") {
      GET(Uri.uri("/brands")).flatMap { req =>
        val routes = new BrandRoutes[IO](dataBrands(b)).routes
        assertHttp(routes, req)(Status.Ok, b.asJson.noSpaces)
      }
    }
  }

  forAll { (b: List[Brand]) =>
    spec("GET brands [ERROR]") {
      GET(Uri.uri("/brands")).flatMap { req =>
        val routes = new BrandRoutes[IO](failingBrands(b)).routes
        assertHttpFailure(routes, req)
      }
    }
  }

}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[Unit] = IO.unit
  def findAll: IO[List[Brand]]          = IO.pure(List.empty)
}
