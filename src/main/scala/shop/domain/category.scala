package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import scala.util.control.NoStackTrace

object category {
  @newtype case class Category(value: NonEmptyString)

  case class InvalidCategory(value: String) extends NoStackTrace
}
