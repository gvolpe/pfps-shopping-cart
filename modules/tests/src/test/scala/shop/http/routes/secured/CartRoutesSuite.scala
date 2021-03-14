package shop.http.routes.secured

import java.util.UUID

import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import shop.generators._
import shop.http.auth.users._
import shop.services.ShoppingCart

import cats.data.Kleisli
import cats.effect._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import squants.market.USD
import suite.HttpSuite

object CartRoutesSuite extends HttpSuite {

  val authUser = CommonUser(User(UserId(UUID.randomUUID), UserName("user")))

  val authMiddleware: AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal) = new TestShoppingCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
  }

  test("GET shopping cart succeeds") {
    forall(cartTotalGen) { ct =>
      GET(Uri.uri("/cart")).flatMap { req =>
        val routes = new CartRoutes[IO](dataCart(ct)).routes(authMiddleware)
        assertHttp(routes, req)(Status.Ok, ct)
      }
    }
  }

  test("POST add item to shopping cart succeeds") {
    forall(cartGen) { c =>
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
