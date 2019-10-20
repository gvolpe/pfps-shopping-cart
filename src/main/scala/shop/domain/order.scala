package shop.domain

import io.estatico.newtype.macros.newtype
import java.{ util => ju }
import scala.util.control.NoStackTrace
import shop.domain.cart._
import shop.domain.item._

object order {
  @newtype case class OrderId(value: ju.UUID)
  @newtype case class PaymentId(value: ju.UUID)

  case class Order(
      id: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: USD
  )

  case class NegativeOrZeroTotalAmount(value: USD) extends NoStackTrace

  case object EmptyCartError extends NoStackTrace
  case class OrderError(cause: String) extends NoStackTrace
  case class PaymentError(cause: String) extends NoStackTrace
}
