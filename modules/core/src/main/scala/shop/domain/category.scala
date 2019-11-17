package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import java.util.UUID

object category {
  @newtype case class CategoryId(value: UUID)
  @newtype case class CategoryName(value: String)

  @newtype case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = value.value.toLowerCase.capitalize.coerce[CategoryName]
  }

  case class Category(uuid: CategoryId, name: CategoryName)
}
