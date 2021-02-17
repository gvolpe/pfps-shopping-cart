package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import shop.domain.cart._
import shop.domain.item._

import io.estatico.newtype.macros.newtype
import squants.market.Money

object order {
  @newtype case class OrderId(value: UUID)
  @newtype case class PaymentId(value: UUID)

  case class Order(
      id: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  case object EmptyCartError extends NoStackTrace
  case class OrderError(cause: String) extends NoStackTrace
  case class PaymentError(cause: String) extends NoStackTrace
}
