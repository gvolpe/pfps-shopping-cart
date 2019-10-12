package shop.programs

import cats.MonadError
import cats.implicits._
import shop.algebras.Orders
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.http.clients.PaymentsClient

final class CheckoutProgram[F[_]: MonadError[?[_], Throwable]](
    paymentsClient: PaymentsClient[F],
    orders: Orders[F]
) {

  // TODO: handle possible errors on remote client? define them.
  def checkout(userId: UserId, cart: Cart): F[Unit] =
    paymentsClient.process(userId, cart).flatMap { orderId =>
      orders.create(userId, orderId, cart)
    }

}
