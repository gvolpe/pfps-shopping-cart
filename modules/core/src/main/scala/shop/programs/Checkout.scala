package shop.programs

import scala.concurrent.duration._

import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.order._
import shop.domain.payment._
import shop.effects.Background
import shop.http.clients.PaymentClient
import shop.retries.{ Retriable, Retry }
import shop.services._

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry._
import squants.market.Money

final case class Checkout[F[_]: Background: Logger: MonadThrow: Retry](
    payments: PaymentClient[F],
    cart: ShoppingCart[F],
    orders: Orders[F],
    policy: RetryPolicy[F]
) {

  private def processPayment(in: Payment): F[PaymentId] =
    Retry[F]
      .retry(policy, Retriable.Payments)(payments.process(in))
      .adaptError {
        case e =>
          PaymentError(Option(e.getMessage).getOrElse("Unknown"))
      }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] = {
    val action =
      Retry[F]
        .retry(policy, Retriable.Orders)(orders.create(userId, paymentId, items, total))
        .adaptError {
          case e => OrderError(e.getMessage)
        }

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.onError {
        case _ =>
          Logger[F].error(
            s"Failed to create order for Payment: ${paymentId.show}. Rescheduling as a background action"
          ) *>
            Background[F].schedule(bgAction(fa), 1.hour)
      }

    bgAction(action)
  }

  private def ensureNonEmpty[A](xs: List[A]): F[NonEmptyList[A]] =
    MonadThrow[F].fromOption(NonEmptyList.fromList(xs), EmptyCartError)

  def process(userId: UserId, card: Card): F[OrderId] =
    cart.get(userId).flatMap {
      case CartTotal(items, total) =>
        for {
          its <- ensureNonEmpty(items)
          pid <- processPayment(Payment(userId, total, card))
          oid <- createOrder(userId, pid, its, total)
          _   <- cart.delete(userId).attempt.void
        } yield oid
    }

}
