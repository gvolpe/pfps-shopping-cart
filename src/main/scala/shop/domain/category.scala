package shop.domain

import io.estatico.newtype.macros.newtype

object category {
  @newtype case class Category(value: String)
}
