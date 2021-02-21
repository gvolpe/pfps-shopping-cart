package shop.domain

import java.util.UUID

import derevo.cats.{ eq => eqv }
import derevo.circe.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined._
import io.estatico.newtype.macros.newtype

object category {
  @derive(decoder, encoder)
  @newtype
  case class CategoryId(value: UUID)

  @derive(decoder, encoder, eqv)
  @newtype
  case class CategoryName(value: String)

  @newtype
  case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(value.value.toLowerCase.capitalize)
  }

  object CategoryParam {
    implicit val jsonDecoder: Decoder[CategoryParam] =
      Decoder.forProduct1("name")(CategoryParam.apply)
  }

  @derive(decoder, encoder)
  case class Category(uuid: CategoryId, name: CategoryName)
}
