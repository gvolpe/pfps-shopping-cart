package shop.programs

import cats.MonadError
import cats.implicits._
import io.estatico.newtype.ops._
import shop.algebras._
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.domain.order.OrderId
import shop.http.clients.PaymentClient
import shop.utils._

final class CheckoutProgram[F[_]: GenUUID: MonadThrow](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {

  // TODO: handle possible errors on remote client? define them.
  def checkout(userId: UserId): F[Unit] =
    for {
      cart <- shoppingCart.findBy(userId)
      paymentId <- paymentClient.process(userId, cart)
      orderId <- GenUUID[F].make.map(_.coerce[OrderId])
      _ <- orders.create(userId, orderId, paymentId, cart)
    } yield ()

}
