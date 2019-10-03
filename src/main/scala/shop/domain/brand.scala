package shop.domain

import io.estatico.newtype.macros.newtype

object brand {
  @newtype case class Brand(value: String)
}
