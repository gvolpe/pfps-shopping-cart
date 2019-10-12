package shop.algebras

import cats.Applicative
import cats.implicits._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.order._

// TODO: Create a program with the interaction between submitting the cart to the payments remote
// service, creating the response in PostgreSQL and resetting the cart for the user.
trait Orders[F[_]] {
  def findBy(userId: UserId): F[List[Order]]
  def create(userId: UserId, orderId: OrderId, paymentId: PaymentId, cart: Cart): F[Unit]
}

object LiveOrders {
  def make[F[_]: Applicative]: F[Orders[F]] =
    new LiveOrders[F].pure[F].widen
}

private class LiveOrders[F[_]: Applicative] extends Orders[F] {

  def findBy(userId: UserId): F[List[Order]] =
    List.empty.pure[F]

  def create(userId: UserId, orderId: OrderId, paymentId: PaymentId, cart: Cart): F[Unit] =
    ().pure[F]

}

