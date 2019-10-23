package shop.domain

import io.estatico.newtype.macros.newtype
import item._
import java.{ util => ju }
import scala.util.control.NoStackTrace
import shop.domain.auth.UserId

object cart {
  @newtype case class Quantity(value: Int)
  @newtype case class Cart(items: Map[ItemId, Quantity])
  @newtype case class CartId(value: ju.UUID)

  case class CartItem(item: Item, quantity: Quantity)
  case class CartTotal(items: List[CartItem], total: USD)

  case class CartNotFound(userId: UserId) extends NoStackTrace
}
