package shop.algebras

import shop.arbitraries._
import shop.config.data.PasswordSalt
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.item._
import shop.domain.order._

import cats.effect._
import cats.implicits._
import ciris._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.ops._
import natchez.Trace.Implicits.noop
import org.scalacheck.Prop._
import skunk._
import squants.market.Money
import suite._

class PostgresTest extends ResourceSuite[Resource[IO, Session[IO]]] {

  lazy val salt = Secret("53kr3t": NonEmptyString).coerce[PasswordSalt]

  override def resources =
    Session.pooled[IO](
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "store",
      max = 10
    )

  withResources { pool =>
    test("Brands") {
      forAll { (brand: Brand) =>
        IOAssertion {
          for {
            b <- LiveBrands.make[IO](pool)
            x <- b.findAll
            _ <- b.create(brand.name)
            y <- b.findAll
            z <- b.create(brand.name).attempt
          } yield assert(
            x.isEmpty && y.count(_.name === brand.name) === 1 && z.isLeft
          )
        }
      }
    }

    test("Categories") {
      forAll { (category: Category) =>
        IOAssertion {
          for {
            c <- LiveCategories.make[IO](pool)
            x <- c.findAll
            _ <- c.create(category.name)
            y <- c.findAll
            z <- c.create(category.name).attempt
          } yield assert(
            x.isEmpty && y.count(_.name === category.name) === 1 && z.isLeft
          )
        }
      }
    }

    test("Items") {
      forAll { (item: Item) =>
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

        IOAssertion {
          for {
            b <- LiveBrands.make[IO](pool)
            c <- LiveCategories.make[IO](pool)
            i <- LiveItems.make[IO](pool)
            x <- i.findAll
            _ <- b.create(item.brand.name)
            d <- b.findAll.map(_.headOption.map(_.uuid))
            _ <- c.create(item.category.name)
            e <- c.findAll.map(_.headOption.map(_.uuid))
            _ <- i.create(newItem(d, e))
            y <- i.findAll
          } yield assert(
            x.isEmpty && y.count(_.name === item.name) === 1
          )
        }
      }
    }

    test("Users") {
      forAll { (username: UserName, password: Password) =>
        IOAssertion {
          for {
            c <- LiveCrypto.make[IO](salt)
            u <- LiveUsers.make[IO](pool, c)
            d <- u.create(username, password)
            x <- u.find(username, password)
            y <- u.find(username, "foo".coerce[Password])
            z <- u.create(username, password).attempt
          } yield assert(
            x.count(_.id === d) === 1 && y.isEmpty && z.isLeft
          )
        }
      }
    }

    test("Orders") {
      forAll {
        (oid: OrderId, pid: PaymentId, un: UserName, pw: Password, items: List[CartItem], price: Money) =>
          IOAssertion {
            for {
              o <- LiveOrders.make[IO](pool)
              c <- LiveCrypto.make[IO](salt)
              u <- LiveUsers.make[IO](pool, c)
              d <- u.create(un, pw)
              x <- o.findBy(d)
              y <- o.get(d, oid)
              i <- o.create(d, pid, items, price)
            } yield assert(
              x.isEmpty && y.isEmpty && i.value.version === 4
            )
          }
      }
    }

  }
}
