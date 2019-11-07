package shop.algebras

import cats.effect._
import cats.implicits._
import ciris._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.ops._
import shop.ItTestSuite
import shop.config.data.PasswordSalt
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.category._
import shop.domain.cart._
import shop.domain.item._
import shop.domain.order._

class PostgreSQLTest extends ItTestSuite {

  spec("Brands") {
    sessionPool.use { pool =>
      val brand = "Foo".coerce[BrandName]
      LiveBrands.make[IO](pool).flatMap { b =>
        for {
          x <- b.findAll
          _ <- b.create(brand)
          y <- b.findAll
          z <- b.create(brand).attempt
        } yield
          assert(
            x.isEmpty && y.count(_.name == brand) == 1 && z.isLeft
          )
      }
    }
  }

  spec("Categories") {
    sessionPool.use { pool =>
      val category = "Foo".coerce[CategoryName]
      LiveCategories.make[IO](pool).flatMap { c =>
        for {
          x <- c.findAll
          _ <- c.create(category)
          y <- c.findAll
          z <- c.create(category).attempt
        } yield
          assert(
            x.isEmpty && y.count(_.name == category) == 1 && z.isLeft
          )
      }
    }
  }

  spec("Items") {
    def newItem(
        bid: Option[BrandId],
        cid: Option[CategoryId]
    ) = CreateItem(
      name = "item".coerce[ItemName],
      description = "desc".coerce[ItemDescription],
      price = USD(12),
      brandId = bid.getOrElse(randomId[BrandId]),
      categoryId = cid.getOrElse(randomId[CategoryId])
    )

    sessionPool.use { pool =>
      for {
        b <- LiveBrands.make[IO](pool)
        c <- LiveCategories.make[IO](pool)
        i <- LiveItems.make[IO](pool)
        x <- i.findAll
        _ <- b.create("bla".coerce[BrandName])
        d <- b.findAll.map(_.headOption.map(_.uuid))
        _ <- c.create("tzy".coerce[CategoryName])
        e <- c.findAll.map(_.headOption.map(_.uuid))
        _ <- i.create(newItem(d, e))
        y <- i.findAll
      } yield
        assert(
          x.isEmpty && y.count(_.name.value == "item") == 1
        )
    }
  }

  spec("Users") {
    val username = "einar".coerce[UserName]
    val password = "123456".coerce[Password]
    val salt     = Secret("53kr3t": NonEmptyString).coerce[PasswordSalt]

    sessionPool.use { pool =>
      for {
        c <- LiveCrypto.make[IO](salt)
        u <- LiveUsers.make[IO](pool, c)
        d <- u.create(username, password)
        x <- u.find(username, password)
        y <- u.find(username, "foo".coerce[Password])
        z <- u.create(username, password).attempt
      } yield
        assert(
          x.count(_.id == d) == 1 && y.isEmpty && z.isLeft
        )
    }
  }

  spec("Orders") {
    val orderId   = randomId[OrderId]
    val paymentId = randomId[PaymentId]

    val item = CartItem(
      Item(
        randomId[ItemId],
        "baz".coerce[ItemName],
        "foo".coerce[ItemDescription],
        USD(100),
        brand = Brand(randomId[BrandId], "Fender".coerce[BrandName]),
        category = Category(randomId[CategoryId], "Guitars".coerce[CategoryName])
      ),
      1.coerce[Quantity]
    )

    val salt = Secret("53kr3t": NonEmptyString).coerce[PasswordSalt]

    sessionPool.use { pool =>
      for {
        o <- LiveOrders.make[IO](pool)
        c <- LiveCrypto.make[IO](salt)
        u <- LiveUsers.make[IO](pool, c)
        d <- u.create("gvolpe".coerce[UserName], "123456".coerce[Password])
        x <- o.findBy(d)
        y <- o.get(d, orderId)
        i <- o.create(d, paymentId, List(item), USD(100))
      } yield
        assert(
          x.isEmpty && y.isEmpty && i.value.toString.nonEmpty
        )
    }
  }

}
