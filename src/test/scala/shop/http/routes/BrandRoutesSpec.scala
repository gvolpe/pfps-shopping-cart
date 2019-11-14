package shop.http.routes

import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import shop.suite.HttpTestSuite
import shop.algebras.Brands
import shop.domain.brand._
import shop.http.json._

class BrandRoutesSpec extends HttpTestSuite {

  val testData = List(Brand(randomId[BrandId], "foo".coerce[BrandName]))

  val dataBrands = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.pure(testData)
  }

  val failingBrands = new TestBrands {
    override def findAll: IO[List[Brand]] =
      IO.raiseError(new Exception("boom"))
  }

  spec("GET brands (no data)") {
    GET(Uri.uri("/brands")).flatMap { req =>
      val routes = new BrandRoutes[IO](new TestBrands).routes
      assertHttp(routes, req)(Status.Ok, "[]")
    }
  }

  spec("GET brands (json data)") {
    GET(Uri.uri("/brands")).flatMap { req =>
      val routes = new BrandRoutes[IO](dataBrands).routes
      assertHttp(routes, req)(Status.Ok, testData.asJson.noSpaces)
    }
  }

  spec("GET brands (failing)") {
    GET(Uri.uri("/brands")).flatMap { req =>
      val routes = new BrandRoutes[IO](failingBrands).routes
      assertHttpFailure(routes, req)
    }
  }

}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[Unit] = IO.unit
  def findAll: IO[List[Brand]]          = IO.pure(List.empty)
}
