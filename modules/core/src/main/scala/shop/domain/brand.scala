package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import shop.ext.http4s.queryParam
import shop.http.params._
import shop.optics.uuid

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.refined._
import io.estatico.newtype.macros.newtype

object brand {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype
  case class BrandId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype
  case class BrandName(value: String) {
    def toBrand(brandId: BrandId): Brand =
      Brand(brandId, this)
  }

  @derive(queryParam)
  @newtype
  case class BrandParam(value: NonEmptyString) {
    def toDomain: BrandName = BrandName(value.value.toLowerCase.capitalize)
  }

  object BrandParam {
    implicit val jsonDecoder: Decoder[BrandParam] =
      Decoder.forProduct1("name")(BrandParam.apply)
  }

  @derive(decoder, encoder, eqv, show)
  case class Brand(uuid: BrandId, name: BrandName)

  @derive(decoder, encoder)
  case class InvalidBrand(value: String) extends NoStackTrace
}
