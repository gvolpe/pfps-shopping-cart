package shop.domain

import io.estatico.newtype.macros.newtype
import java.{ util => ju }
//import squants.market.USD

object item {

  @newtype case class ItemId(value: ju.UUID)
  @newtype case class ItemName(value: String)
  @newtype case class ItemDescription(value: String)
  @newtype case class USD(value: BigDecimal)

  case class Item(
      uuid: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: USD
  )

  // ----- Create item ------

  case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: USD
  )

}
