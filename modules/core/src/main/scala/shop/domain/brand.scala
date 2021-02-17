package shop.domain

import java.util.UUID

import scala.util.control.NoStackTrace

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object brand {
  @newtype case class BrandId(value: UUID)

  @newtype case class BrandName(value: String) {
    def toBrand(brandId: BrandId): Brand =
      Brand(brandId, this)
  }

  @newtype case class BrandParam(value: NonEmptyString) {
    def toDomain: BrandName = BrandName(value.value.toLowerCase.capitalize)
  }

  case class Brand(uuid: BrandId, name: BrandName)

  case class InvalidBrand(value: String) extends NoStackTrace
}
