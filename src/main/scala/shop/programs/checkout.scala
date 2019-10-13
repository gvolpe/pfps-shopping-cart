package shop.programs

import cats.MonadError
import cats.effect.Timer
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import retry._
import retry.CatsEffect._
import retry.RetryDetails._
import retry.RetryPolicies._
import scala.concurrent.duration._
import shop.algebras._
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.domain.order._
import shop.http.clients.PaymentClient
import shop.typeclasses.Background
import shop.utils._

final class CheckoutProgram[F[_]: Background: Logger: MonadThrow: Timer](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {

  private val retryPolicy = limitRetries[F](3) |+| exponentialBackoff[F](10.milliseconds)

  private def logError(action: String)(e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case r: WillDelayAndRetry =>
        Logger[F].info(
          s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
        )
      case g: GivingUp =>
        Logger[F].info(s"Giving up on $action after ${g.totalRetries} retries.")
    }

  private def processPayment(userId: UserId, cart: Cart): F[PaymentId] = {
    val action = retryingOnAllErrors[PaymentId](
      policy = retryPolicy,
      onError = logError("Payments")
    )(paymentClient.process(userId, cart))

    action.adaptError {
      case e => PaymentError(e.getMessage)
    }
  }

  private def createOrder(userId: UserId, paymentId: PaymentId, cart: Cart): F[OrderId] = {
    val action = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, cart))

    action
      .adaptError {
        case e => OrderError(e.getMessage)
      }
      .onError {
        case _ =>
          Background[F].run(
            Timer[F].sleep(1.hour) *> action
          )
      }
  }

  def checkout(userId: UserId): F[OrderId] =
    shoppingCart.findBy(userId).flatMap {
      case cart if cart.items.isEmpty =>
        EmptyCartError.raiseError[F, OrderId]
      case cart =>
        processPayment(userId, cart).flatMap { paymentId =>
          createOrder(userId, paymentId, cart)
        }
    }

}
