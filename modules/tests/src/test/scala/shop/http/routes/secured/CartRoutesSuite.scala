package shop.http.routes.secured

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
import org.http4s.syntax.literals._
import squants.market.USD
import suite.HttpSuite

object CartRoutesSuite extends HttpSuite {

  def authMiddleware(authUser: CommonUser): AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal) = new TestShoppingCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
  }

  test("GET shopping cart succeeds") {
    val gen = for {
      u <- commonUserGen
      c <- cartTotalGen
    } yield u -> c

    forall(gen) {
      case (user, ct) =>
        val req    = GET(uri"/cart")
        val routes = CartRoutes[IO](dataCart(ct)).routes(authMiddleware(user))
        expectHttpBodyAndStatus(routes, req)(ct, Status.Ok)
    }
  }

  test("POST add item to shopping cart succeeds") {
    val gen = for {
      u <- commonUserGen
      c <- cartGen
    } yield u -> c

    forall(gen) {
      case (user, c) =>
        val req    = POST(c, uri"/cart")
        val routes = CartRoutes[IO](new TestShoppingCart).routes(authMiddleware(user))
        expectHttpStatus(routes, req)(Status.Created)
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
