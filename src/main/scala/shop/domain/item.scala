package shop.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
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
        Brand(ju.UUID.randomUUID.coerce[BrandId], "Ibanez".coerce[BrandName]), // FIXME: Hardcoded
        Category(ju.UUID.randomUUID.coerce[CategoryId], "Guitars".coerce[CategoryName]) // FIXME: Hardcoded
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

  // ----- Update item ------

  @newtype case class ItemIdParam(value: String Refined Uuid)
  @newtype case class PriceParam(value: String Refined ValidBigDecimal)

  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toDomain: UpdateItem =
      UpdateItem(
        ju.UUID.fromString(id.value.value).coerce[ItemId],
        BigDecimal(price.value.value).coerce[USD]
      )
  }

  case class UpdateItem(
      id: ItemId,
      price: USD
  )

}
