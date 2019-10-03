package shop.domain

import io.estatico.newtype.macros.newtype
import item._
import java.{util => ju}

object cart {
  @newtype case class Cart(items: Map[ItemId, Int])
  @newtype case class CartId(value: ju.UUID)

  case class CartItem(item: Item, quantity: Int)
}
