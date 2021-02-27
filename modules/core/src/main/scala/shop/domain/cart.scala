package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import shop.domain.auth.UserId
import shop.optics.uuid

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.circe.{ Decoder, Encoder }
import io.estatico.newtype.macros.newtype
import squants.market.Money

import item._

object cart {
  @derive(decoder, encoder, eqv, show)
  @newtype
  case class Quantity(value: Int)

  @derive(eqv, show)
  @newtype
  case class Cart(items: Map[ItemId, Quantity])
  object Cart {
    implicit val jsonEncoder: Encoder[Cart] =
      Encoder.forProduct1("items")(_.items)

    implicit val jsonDecoder: Decoder[Cart] =
      Decoder.forProduct1("items")(Cart.apply)
  }

  @derive(decoder, encoder, eqv, show, uuid)
  @newtype
  case class CartId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  case class CartItem(item: Item, quantity: Quantity)

  @derive(decoder, encoder, eqv, show)
  case class CartTotal(items: List[CartItem], total: Money)

  @derive(decoder, encoder)
  case class CartNotFound(userId: UserId) extends NoStackTrace
}
