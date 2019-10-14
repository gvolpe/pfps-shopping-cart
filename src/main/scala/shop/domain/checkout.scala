package shop.domain

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object checkout {
  type CardNumber     = Long Refined Size[16]
  type CardExpiration = Int Refined Size[4]
  type CardCCV        = Int Refined Size[3]

  @newtype case class CardName(value: NonEmptyString) // TODO: validate with regex?

  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      ccv: CardCCV
  )
}
