package shop.algebras

import shop.config.data.PasswordSalt
import shop.domain._
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.category._
import shop.domain.item._
import shop.generators._

import cats.effect._
import cats.implicits._
import ciris._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.ops._
import natchez.Trace.Implicits.noop
import org.scalacheck.Gen
import skunk._
import weaver.IOSuite
import weaver.scalacheck.{CheckConfig, Checkers}

object PostgresTest extends IOSuite with Checkers {

  // For it:tests, one test is enough
  override def checkConfig: CheckConfig = CheckConfig.default.copy(minimumSuccessful = 1)

  lazy val salt = Secret("53kr3t": NonEmptyString).coerce[PasswordSalt]

  override type Res = Resource[IO, Session[IO]]
  override def sharedResource: Resource[IO, Res] =
    Session.pooled[IO](
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "store",
      max = 10
    )

  test("Brands") { pool =>
    forall(brandGen) { brand =>
      val b = Brands.make[IO](pool)
      for {
        x <- b.findAll
        _ <- b.create(brand.name)
        y <- b.findAll
        z <- b.create(brand.name).attempt
      } yield expect.all(x.isEmpty, y.count(_.name === brand.name) === 1, z.isLeft)
    }
  }

  test("Categories") { pool =>
    forall(categoryGen) { category =>
      val c = Categories.make[IO](pool)
      for {
        x <- c.findAll
        _ <- c.create(category.name)
        y <- c.findAll
        z <- c.create(category.name).attempt
      } yield expect.all(x.isEmpty, y.count(_.name === category.name) === 1, z.isLeft)
    }
  }

  test("Items") { pool =>
    forall(itemGen) { item =>
      def newItem(
          bid: Option[BrandId],
          cid: Option[CategoryId]
      ) = CreateItem(
        name = item.name,
        description = item.description,
        price = item.price,
        brandId = bid.getOrElse(item.brand.uuid),
        categoryId = cid.getOrElse(item.category.uuid)
      )

      val b = Brands.make[IO](pool)
      val c = Categories.make[IO](pool)
      val i = Items.make[IO](pool)

      for {
        x <- i.findAll
        _ <- b.create(item.brand.name)
        d <- b.findAll.map(_.headOption.map(_.uuid))
        _ <- c.create(item.category.name)
        e <- c.findAll.map(_.headOption.map(_.uuid))
        _ <- i.create(newItem(d, e))
        y <- i.findAll
      } yield expect.all(x.isEmpty, y.count(_.name === item.name) === 1)
    }
  }

  test("Users") { pool =>
    val gen = for {
      u <- userNameGen
      p <- passwordGen
    } yield u -> p

    forall(gen) {
      case (username, password) =>
        for {
          c <- Crypto.make[IO](salt)
          u = Users.make[IO](pool, c)
          d <- u.create(username, password)
          x <- u.find(username, password)
          y <- u.find(username, "foo".coerce[Password])
          z <- u.create(username, password).attempt
        } yield expect.all(x.count(_.id === d) === 1, y.isEmpty, z.isLeft)
    }
  }

  test("Orders") { pool =>
    val gen = for {
      oid <- orderIdGen
      pid <- paymentIdGen
      un <- userNameGen
      pw <- passwordGen
      it <- Gen.listOf(cartItemGen)
      pr <- moneyGen
    } yield (oid, pid, un, pw, it, pr)

    forall(gen) {
      case (oid, pid, un, pw, items, price) =>
        val o = Orders.make[IO](pool)
        for {
          c <- Crypto.make[IO](salt)
          u = Users.make[IO](pool, c)
          d <- u.create(un, pw)
          x <- o.findBy(d)
          y <- o.get(d, oid)
          i <- o.create(d, pid, items, price)
        } yield expect.all(x.isEmpty, y.isEmpty, i.value.version === 4)
    }
  }

}
