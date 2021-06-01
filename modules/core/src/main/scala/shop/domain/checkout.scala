package shop.domain

import shop.ext.refined._

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.api._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.cats._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import io.circe.Decoder
import io.circe.refined._
import io.estatico.newtype.macros.newtype

object checkout {
  type Rgx = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"

  type CardNamePred       = String Refined MatchesRegex[Rgx]
  type CardNumberPred     = Long Refined Size[16]
  type CardExpirationPred = String Refined (Size[4] And ValidInt)
  type CardCVVPred        = Int Refined Size[3]

  @derive(decoder, encoder, show)
  @newtype
  case class CardName(value: CardNamePred)

  @derive(encoder, show)
  @newtype
  case class CardNumber(value: CardNumberPred)
  object CardNumber {
    implicit val jsonDecoder: Decoder[CardNumber] =
      decoderOf[Long, Size[16]].map(CardNumber(_))
  }

  @derive(encoder, show)
  @newtype
  case class CardExpiration(value: CardExpirationPred)
  object CardExpiration {
    implicit val jsonDecoder: Decoder[CardExpiration] =
      decoderOf[String, Size[4] And ValidInt].map(CardExpiration(_))
  }

  @derive(encoder, show)
  @newtype
  case class CardCVV(value: CardCVVPred)
  object CardCVV {
    implicit val jsonDecoder: Decoder[CardCVV] =
      decoderOf[Int, Size[3]].map(CardCVV(_))
  }

  @derive(decoder, encoder, show)
  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      cvv: CardCVV
  )
}
