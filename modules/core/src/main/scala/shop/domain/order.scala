package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import shop.domain.cart._
import shop.domain.item._

import derevo.cats.{ eq => eqv }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.circe.Decoder
import io.estatico.newtype.macros.newtype
import squants.market.Money

object order {
  @derive(decoder, encoder, eqv)
  @newtype
  case class OrderId(value: UUID)

  @derive(encoder)
  @newtype
  case class PaymentId(value: UUID)
  object PaymentId {
    implicit val jsonDecoder: Decoder[PaymentId] =
      Decoder.forProduct1("paymentId")(PaymentId.apply)
  }

  @derive(decoder, encoder)
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
