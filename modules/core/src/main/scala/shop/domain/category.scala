package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import java.util.UUID

object category {
  @newtype case class CategoryId(value: UUID)
  @newtype case class CategoryName(value: String)

  @newtype case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(value.value.toLowerCase.capitalize)
  }

  case class Category(uuid: CategoryId, name: CategoryName)
}
