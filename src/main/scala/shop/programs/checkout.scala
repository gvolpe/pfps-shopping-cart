package shop.programs

import cats.MonadError
import cats.implicits._
import shop.algebras._
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.http.clients.PaymentClient
import shop.utils._

final class CheckoutProgram[F[_]: MonadThrow](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {

  // TODO: handle possible errors on remote client? define them.
  def checkout(userId: UserId): F[Unit] =
    for {
      cart <- shoppingCart.findBy(userId)
      paymentId <- paymentClient.process(userId, cart)
      _ <- orders.create(userId, paymentId, cart)
    } yield ()

}
