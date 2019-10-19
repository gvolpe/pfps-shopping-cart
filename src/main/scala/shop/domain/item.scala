package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
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

  @newtype case class ItemNameParam(value: NonEmptyString)
  @newtype case class ItemDescriptionParam(value: NonEmptyString)

  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: USD
  ) {
    def toDomain: CreateItem =
      CreateItem(
        name.value.value.coerce[ItemName],
        description.value.value.coerce[ItemDescription],
        price,
        Brand("Ibanez"), // FIXME: Hardcoded
        Category("Guitars")
      )
  }

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: USD,
      brand: Brand,
      category: Category
  ) {
    def toItem(itemId: ItemId) =
      Item(itemId, name, description, price, brand, category)
  }

}
