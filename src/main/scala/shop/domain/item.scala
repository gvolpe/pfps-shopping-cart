package shop.domain

import io.estatico.newtype.macros.newtype
import java.{ util => ju }
//import squants.market.USD
import shop.domain.brand._
import shop.domain.category._

object item {

  @newtype case class ItemId(value: ju.UUID)
  @newtype case class ItemName(value: String)
  @newtype case class ItemDescription(value: String)
  @newtype case class USD(value: BigDecimal)

  case class Item(
      uuid: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: USD,
      brand: Brand,
      category: Category
  )

  // ----- Create item ------

  case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: USD
  )

}
