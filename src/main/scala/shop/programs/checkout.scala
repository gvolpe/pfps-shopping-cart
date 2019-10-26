package shop.programs

import cats.effect.Timer
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.estatico.newtype.ops._
import retry._
import retry.CatsEffect._
import retry.RetryDetails._
import retry.RetryPolicies._
import scala.concurrent.duration._
import shop.algebras._
import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.effects._
import shop.http.clients.PaymentClient

final class CheckoutProgram[F[_]: Background: Logger: MonadThrow: Timer](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {

  private val retryPolicy = limitRetries[F](3) |+| exponentialBackoff[F](10.milliseconds)

  private def logError(action: String)(e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case r: WillDelayAndRetry =>
        Logger[F].error(
          s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
        )
      case g: GivingUp =>
        Logger[F].error(s"Giving up on $action after ${g.totalRetries} retries.")
    }

  private def processPayment(userId: UserId, total: USD, card: Card): F[PaymentId] = {
    val action = retryingOnAllErrors[PaymentId](
      policy = retryPolicy,
      onError = logError("Payments")
    )(paymentClient.process(userId, total, card))

    action.adaptError {
      case e => PaymentError(e.getMessage)
    }
  }

  private def createOrder(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: USD): F[OrderId] = {
    val action = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, items, total))

    action
      .adaptError {
        case e => OrderError(e.getMessage)
      }
      .onError {
        case _ =>
          Logger[F].error(s"Failed to create order for Payment: ${paymentId}. Re-scheduling as a background action") *>
            Background[F].schedule(1.hour, action)
      }
  }

  def checkout(userId: UserId, card: Card): F[OrderId] =
    shoppingCart.get(userId).flatMap {
      case CartTotal(items, _) if items.isEmpty =>
        EmptyCartError.raiseError[F, OrderId]
      case CartTotal(items, total) =>
        for {
          pid <- processPayment(userId, total, card)
          order <- createOrder(userId, pid, items, total)
          _ <- shoppingCart.delete(userId).attempt.void
        } yield order
    }

}
