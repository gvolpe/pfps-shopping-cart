package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import java.{util => ju}

object category {
  @newtype case class CategoryId(value: ju.UUID)
  @newtype case class CategoryName(value: String)

  @newtype case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = value.value.coerce[CategoryName]
  }

  case class Category(uuid: CategoryId, name: CategoryName)
}
