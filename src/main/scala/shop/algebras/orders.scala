package shop.algebras

import shop.domain.order._
import shop.domain.cart._
import shop.http.auth.roles.UserId

// TODO: Create a program with the interaction between submitting the cart to the payments remote
// service, creating the response in PostgreSQL and resetting the cart for the user.
trait Orders[F[_]] {
  def getAll(userId: UserId): F[List[Order]]
  def create(userId: UserId, orderId: OrderId, cart: Cart): F[Unit]
}
