package shop.domain

import java.util.UUID

import shop.domain.brand._
import shop.domain.cart.{ CartItem, Quantity }
import shop.domain.category._
import shop.optics.uuid

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import squants.market._

object item {

  @derive(decoder, encoder, keyDecoder, keyEncoder, eqv, show, uuid)
  @newtype
  case class ItemId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class ItemName(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class ItemDescription(value: String)

  @derive(decoder, encoder, eqv, show)
  case class Item(
      uuid: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: Brand,
      category: Category
  ) {
    def cart(q: Quantity): CartItem =
      CartItem(this, q)
  }

  // ----- Create item ------

  @derive(decoder, encoder, show)
  @newtype
  case class ItemNameParam(value: NonEmptyString)

  @derive(decoder, encoder, show)
  @newtype
  case class ItemDescriptionParam(value: NonEmptyString)

  @derive(decoder, encoder, show)
  @newtype
  case class PriceParam(value: String Refined ValidBigDecimal)

  @derive(decoder, encoder, show)
  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: PriceParam,
      brandId: BrandId,
      categoryId: CategoryId
  ) {
    def toDomain: CreateItem =
      CreateItem(
        ItemName(name.value),
        ItemDescription(description.value),
        USD(BigDecimal(price.value)),
        brandId,
        categoryId
      )
  }

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  // ----- Update item ------

  @derive(decoder, encoder)
  @newtype
  case class ItemIdParam(value: String Refined Uuid)

  @derive(decoder, encoder)
  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toDomain: UpdateItem =
      UpdateItem(
        ItemId(UUID.fromString(id.value)),
        USD(BigDecimal(price.value))
      )
  }

  @derive(decoder, encoder)
  case class UpdateItem(
      id: ItemId,
      price: Money
  )

}
