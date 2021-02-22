package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import derevo.cats.{ eq => eqv }
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined._
import io.estatico.newtype.macros.newtype

object brand {
  @derive(decoder, encoder)
  @newtype
  case class BrandId(value: UUID)

  @derive(decoder, encoder, eqv)
  @newtype
  case class BrandName(value: String) {
    def toBrand(brandId: BrandId): Brand =
      Brand(brandId, this)
  }

  @newtype
  case class BrandParam(value: NonEmptyString) {
    def toDomain: BrandName = BrandName(value.value.toLowerCase.capitalize)
  }

  object BrandParam {
    implicit val jsonDecoder: Decoder[BrandParam] =
      Decoder.forProduct1("name")(BrandParam.apply)
  }

  @derive(decoder, encoder)
  case class Brand(uuid: BrandId, name: BrandName)

  @derive(decoder, encoder)
  case class InvalidBrand(value: String) extends NoStackTrace
}
