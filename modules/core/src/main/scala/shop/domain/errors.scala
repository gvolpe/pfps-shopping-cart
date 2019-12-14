package shop.domain

import scala.util.control.NoStackTrace
import shop.domain.auth.UserId

object errors {

  sealed trait CheckoutError extends NoStackTrace
  case class CartNotFound(userId: UserId) extends CheckoutError
  case object EmptyCartError extends CheckoutError
  case class OrderError(cause: String) extends CheckoutError
  case class PaymentError(cause: String) extends CheckoutError

}
