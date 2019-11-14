package shop.http.routes.secured

import cats.data.{ Kleisli, OptionT }
import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.estatico.newtype.ops._
import java.util.UUID
import org.http4s._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import shop.suite.HttpTestSuite
import shop.algebras.ShoppingCart
import shop.arbitraries._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import shop.http.auth.roles._
import shop.http.json._

class CartRoutesSpec extends HttpTestSuite {

  val authUser = User(randomId[UserId], "user".coerce[UserName]).coerce[CommonUser]

  val authMiddleware: AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli(_ => OptionT.pure[IO](authUser)))

  def dataCart(cartTotal: CartTotal) = new TestShoppingCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
  }

  forAll { (ct: CartTotal, id: UUID) =>
    spec(s"GET shopping cart [OK] - $id") {
      GET(Uri.uri("/cart")).flatMap { req =>
        val routes = new CartRoutes[IO](dataCart(ct)).routes(authMiddleware)
        assertHttp(routes, req)(Status.Ok, ct.asJson.noSpaces)
      }
    }
  }

  forAll { (c: Cart, id: UUID) =>
    spec(s"POST add item to shopping cart [OK] - $id") {
      POST(c, Uri.uri("/cart")).flatMap { req =>
        val routes = new CartRoutes[IO](new TestShoppingCart).routes(authMiddleware)
        assertHttpStatus(routes, req)(Status.Created)
      }
    }
  }

}

protected class TestShoppingCart extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  def get(userId: UserId): IO[CartTotal] =
    IO.pure(CartTotal(List.empty, USD(0)))
  def delete(userId: UserId): IO[Unit]                     = IO.unit
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = IO.unit
  def update(userId: UserId, cart: Cart): IO[Unit]         = IO.unit
}
