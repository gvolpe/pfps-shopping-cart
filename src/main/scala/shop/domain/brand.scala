package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import scala.util.control.NoStackTrace

object brand {
  @newtype case class Brand(value: String)

  @newtype case class BrandParam(value: NonEmptyString) {
    def asBrand: Brand = this.value.value.coerce[Brand]
  }

  case class InvalidBrand(value: String) extends NoStackTrace
}
