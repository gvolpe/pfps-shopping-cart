package shop.programs

import scala.concurrent.duration._

import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.order._
import shop.domain.payment._
import shop.effects._
import shop.http.clients.PaymentClient
import shop.services._

import cats.effect.{ MonadThrow, Timer }
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryDetails._
import retry._
import squants.market.Money

final class Checkout[F[_]: Background: Logger: MonadThrow: Timer](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F],
    retryPolicy: RetryPolicy[F]
) {

  private def logError(action: String)(e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case r: WillDelayAndRetry =>
        Logger[F].error(
          s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
        )
      case g: GivingUp =>
        Logger[F].error(s"Giving up on $action after ${g.totalRetries} retries.")
    }

  private def processPayment(payment: Payment): F[PaymentId] = {
    val action = retryingOnAllErrors[PaymentId](
      policy = retryPolicy,
      onError = logError("Payments")
    )(paymentClient.process(payment))

    action.adaptError {
      case e =>
        PaymentError(Option(e.getMessage).getOrElse("Unknown"))
    }
  }

  private def createOrder(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId] = {
    val action = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, items, total))

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.adaptError {
          case e => OrderError(e.getMessage)
        }
        .onError {
          case _ =>
            Logger[F].error(s"Failed to create order for Payment: ${paymentId}. Rescheduling as a background action") *>
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
            order <- createOrder(userId, pid, items, total)
            _ <- shoppingCart.delete(userId).attempt.void
          } yield order
      }

}
