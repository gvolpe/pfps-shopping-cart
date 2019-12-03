package shop.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import java.util.UUID
//import squants.market.USD
import shop.domain.brand._
import shop.domain.category._

object item {

  @newtype case class ItemId(value: UUID)
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
      price: USD,
      brandId: BrandId,
      categoryId: CategoryId
  ) {
    def toDomain: CreateItem =
      CreateItem(
        ItemName(name.value.value),
        ItemDescription(description.value.value),
        price,
        brandId,
        categoryId
      )
  }

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: USD,
      brandId: BrandId,
      categoryId: CategoryId
  )

  // ----- Update item ------

  @newtype case class ItemIdParam(value: String Refined Uuid)
  @newtype case class PriceParam(value: String Refined ValidBigDecimal)

  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toDomain: UpdateItem =
      UpdateItem(
        ItemId(UUID.fromString(id.value.value)),
        USD(BigDecimal(price.value.value))
      )
  }

  case class UpdateItem(
      id: ItemId,
      price: USD
  )

}
