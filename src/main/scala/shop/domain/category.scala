package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object category {
  @newtype case class Category(value: String)

  @newtype case class CategoryParam(value: NonEmptyString) {
    def toDomain: Category = value.value.coerce[Category]
  }
}
