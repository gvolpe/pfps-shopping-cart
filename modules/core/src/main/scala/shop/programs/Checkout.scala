package shop.programs

import scala.concurrent.duration._

import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.order._
import shop.domain.payment._
import shop.effects.Background
import shop.http.clients.PaymentClient
import shop.retries.{ Retriable, RetryHandler }
import shop.services._

import cats.effect.Temporal
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry._
import squants.market.Money

final case class Checkout[F[_]: Background: Logger: RetryHandler: Temporal](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F],
    retryPolicy: RetryPolicy[F]
) {

  private def retry[A](retriable: Retriable)(fa: F[A]): F[A] =
    retryingOnAllErrors[A](retryPolicy, RetryHandler[F].onError(retriable))(fa)

  private def processPayment(payment: Payment): F[PaymentId] =
    retry(Retriable.Payments)(paymentClient.process(payment))
      .adaptError {
        case e =>
          PaymentError(Option(e.getMessage).getOrElse("Unknown"))
      }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId] = {
    val action =
      retry(Retriable.Orders)(orders.create(userId, paymentId, items, total))
        .adaptError {
          case e => OrderError(e.getMessage)
        }

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.onError {
        case _ =>
          Logger[F].error(
            s"Failed to create order for Payment: ${paymentId}. Rescheduling as a background action"
          ) *>
            Background[F].schedule(bgAction(fa), 1.hour)
      }

    bgAction(action)
  }

  def process(userId: UserId, card: Card): F[OrderId] =
    shoppingCart
      .get(userId)
      .ensure(EmptyCartError)(_.items.nonEmpty)
      .flatMap {
        case CartTotal(items, total) =>
          for {
            pid <- processPayment(Payment(userId, total, card))
            oid <- createOrder(userId, pid, items, total)
            _   <- shoppingCart.delete(userId).attempt.void
          } yield oid
      }

}
