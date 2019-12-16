package shop.algebras

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits.{ catsSyntaxEq => _, _ }
import ciris.Secret
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{ RedisClient, RedisURI }
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.ops._
import java.util.UUID
import pdi.jwt._
import scala.concurrent.duration._
import shop.arbitraries._
import shop.config.data._
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.category._
import shop.domain.cart._
import shop.domain.item._
import shop.logger.NoOp
import shop.http.auth.users._
import suite.ResourceSuite

class RedisTest extends ResourceSuite[RedisCommands[IO, String, String]] {

  // For it:tests, one test is enough
  val MaxTests: PropertyCheckConfigParam = MinSuccessful(1)

  override def resources =
    for {
      uri <- Resource.liftF(RedisURI.make[IO]("redis://localhost"))
      client <- RedisClient[IO](uri)
      cmd <- Redis[IO, String, String](client, RedisCodec.Utf8, uri)
    } yield cmd

  lazy val Exp         = 30.seconds.coerce[ShoppingCartExpiration]
  lazy val tokenConfig = Secret("bar": NonEmptyString).coerce[JwtSecretKeyConfig]
  lazy val tokenExp    = 30.seconds.coerce[TokenExpiration]
  lazy val jwtClaim    = JwtClaim("test")
  lazy val userJwtAuth = JwtAuth.hmac("bar", JwtAlgorithm.HS256).coerce[UserJwtAuth]

  withResources { cmd =>
    forAll(MaxTests) { (uid: UserId, it1: Item, it2: Item, q1: Quantity, q2: Quantity) =>
      spec("Shopping Cart") {
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
            } yield
              assert(
                x.items.isEmpty && y.items.size === 2 &&
                z.items.size == 1 && v.items.isEmpty &&
                w.items.headOption.fold(false)(_.quantity === q2)
              )
          }
        }
      }
    }

    forAll(MaxTests) { (un1: UserName, un2: UserName, pw: Password) =>
      spec("Authentication") {
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
        } yield
          assert(
            x.isEmpty && e.isRight && f.isRight && y.isEmpty &&
            w.fold(false)(_.value.name === un1)
          )
      }
    }
  }

}

protected class TestUsers(un: UserName) extends Users[IO] {
  def find(username: UserName, password: Password): IO[Option[User]] =
    (username == un).guard[Option].as(User(UUID.randomUUID.coerce[UserId], un)).pure[IO]
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
      val brand    = Brand(item.brandId, "foo".coerce[BrandName])
      val category = Category(item.categoryId, "foo".coerce[CategoryName])
      val newItem  = Item(id, item.name, item.description, item.price, brand, category)
      ref.update(_.updated(id, newItem))
    }
  def update(item: UpdateItem): IO[Unit] =
    ref.update(x => x.get(item.id).fold(x)(i => x.updated(item.id, i.copy(price = item.price))))
}
