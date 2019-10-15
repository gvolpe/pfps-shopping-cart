package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import scala.util.control.NoStackTrace

object brand {
  @newtype case class Brand(value: NonEmptyString)

  case class InvalidBrand(value: String) extends NoStackTrace
}
