package shop.algebras

import java.util.UUID

import scala.concurrent.duration._

import shop.arbitraries._
import shop.config.data._
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.item._
import shop.http.auth.users._
import shop.logger.NoOp

import cats.Eq
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import ciris.Secret
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Prop._
import pdi.jwt._
import suite._

class RedisTest extends ResourceSuite[RedisCommands[IO, String, String]] {

  override def resources =
    Redis[IO].utf8("redis://localhost")

  lazy val Exp         = ShoppingCartExpiration(30.seconds)
  lazy val tokenConfig = JwtSecretKeyConfig(Secret("bar": NonEmptyString))
  lazy val tokenExp    = TokenExpiration(30.seconds)
  lazy val jwtClaim    = JwtClaim("test")
  lazy val userJwtAuth = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  withResources { cmd =>
    test("Shopping Cart") {
      forAll { (uid: UserId, it1: Item, it2: Item, q1: Quantity, q2: Quantity) =>
        IOAssertion {
          Ref.of[IO, Map[ItemId, Item]](Map(it1.uuid -> it1, it2.uuid -> it2)).flatMap { ref =>
            val items = new TestItems(ref)
            LiveShoppingCart.make[IO](items, cmd, Exp).flatMap { c =>
              for {
                x <- c.get(uid)
                _ <- c.add(uid, it1.uuid, q1)
                _ <- c.add(uid, it2.uuid, q1)
                y <- c.get(uid)
                _ <- c.removeItem(uid, it1.uuid)
                z <- c.get(uid)
                _ <- c.update(uid, Cart(Map(it2.uuid -> q2)))
                w <- c.get(uid)
                _ <- c.delete(uid)
                v <- c.get(uid)
              } yield assert(
                x.items.isEmpty && y.items.size === 2 &&
                  z.items.size === 1 && v.items.isEmpty &&
                  w.items.headOption.fold(false)(_.quantity === q2)
              )
            }
          }
        }
      }
    }

    test("Authentication") {
      forAll { (un1: UserName, un2: UserName, pw: Password) =>
        IOAssertion {
          for {
            t <- LiveTokens.make[IO](tokenConfig, tokenExp)
            a <- LiveAuth.make(tokenExp, t, new TestUsers(un2), cmd)
            u <- LiveUsersAuth.make[IO](cmd)
            x <- u.findUser(JwtToken("invalid"))(jwtClaim)
            j <- a.newUser(un1, pw)
            e <- jwtDecode[IO](j, userJwtAuth.value).attempt
            k <- a.login(un2, pw)
            f <- jwtDecode[IO](k, userJwtAuth.value).attempt
            _ <- a.logout(k, un2)
            y <- u.findUser(k)(jwtClaim)
            w <- u.findUser(j)(jwtClaim)
          } yield assert(
            x.isEmpty && e.isRight && f.isRight && y.isEmpty &&
              w.fold(false)(_.value.name === un1)
          )
        }
      }
    }
  }

}

protected class TestUsers(un: UserName) extends Users[IO] {
  def find(username: UserName, password: Password): IO[Option[User]] =
    Eq[UserName].eqv(username, un).guard[Option].as(User(UserId(UUID.randomUUID), un)).pure[IO]
  def create(username: UserName, password: Password): IO[UserId] =
    GenUUID[IO].make[UserId]
}

protected class TestItems(ref: Ref[IO, Map[ItemId, Item]]) extends Items[IO] {
  def findAll: IO[List[Item]] =
    ref.get.map(_.values.toList)
  def findBy(brand: BrandName): IO[List[Item]] =
    ref.get.map(_.values.filter(_.brand.name == brand).toList)
  def findById(itemId: ItemId): IO[Option[Item]] =
    ref.get.map(_.get(itemId))
  def create(item: CreateItem): IO[Unit] =
    GenUUID[IO].make[ItemId].flatMap { id =>
      val brand    = Brand(item.brandId, BrandName("foo"))
      val category = Category(item.categoryId, CategoryName("foo"))
      val newItem  = Item(id, item.name, item.description, item.price, brand, category)
      ref.update(_.updated(id, newItem))
    }
  def update(item: UpdateItem): IO[Unit] =
    ref.update(x => x.get(item.id).fold(x)(i => x.updated(item.id, i.copy(price = item.price))))
}
