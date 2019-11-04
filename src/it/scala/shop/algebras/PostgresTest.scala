package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import org.scalatest.FunSuite
import shop.ItTestSuite
import shop.domain.brand._
import shop.domain.category._

class PostgreSQLTest extends ItTestSuite {

  spec("Brands") {
    sessionPool.use { pool =>
      LiveBrands.make[IO](pool).flatMap { b =>
        for {
          x <- b.findAll
          _ <- b.create("Foo".coerce[BrandName])
          y <- b.findAll
        } yield
          assert(
            x.isEmpty && y.exists(_.name.value == "Foo")
          )
      }
    }
  }

  spec("Categories") {
    sessionPool.use { pool =>
      LiveCategories.make[IO](pool).flatMap { c =>
        for {
          x <- c.findAll
          _ <- c.create("Foo".coerce[CategoryName])
          y <- c.findAll
        } yield
          assert(
            x.isEmpty && y.exists(_.name.value == "Foo")
          )
      }
    }
  }

}
