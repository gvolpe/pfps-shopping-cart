package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import shop.domain.cart._
import shop.domain.item._
import shop.optics.uuid

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.estatico.newtype.macros.newtype
import squants.market.Money

object order {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype
  case class OrderId(value: UUID)

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype
  case class PaymentId(value: UUID)

  @derive(decoder, encoder)
  case class Order(
      id: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  @derive(show)
  case object EmptyCartError extends NoStackTrace

  @derive(show)
  sealed trait OrderOrPaymentError extends NoStackTrace {
    def cause: String
  }

  @derive(eqv, show)
  case class OrderError(cause: String)   extends OrderOrPaymentError
  case class PaymentError(cause: String) extends OrderOrPaymentError
}
