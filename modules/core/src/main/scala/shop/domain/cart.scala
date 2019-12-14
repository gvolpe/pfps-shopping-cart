package shop.domain

import io.estatico.newtype.macros.newtype
import item._
import java.util.UUID
import squants.market.Money

object cart {
  @newtype case class Quantity(value: Int)
  @newtype case class Cart(items: Map[ItemId, Quantity])
  @newtype case class CartId(value: UUID)

  case class CartItem(item: Item, quantity: Quantity)
  case class CartTotal(items: List[CartItem], total: Money)
}
