package shop.domain

import eu.timepit.refined._
import eu.timepit.refined.auto._
import io.estatico.newtype.ops._
import io.scalaland.chimney.dsl._
import shop.domain.brand._

object conversions {

  //def toItem(createItem: CreateItem): Item =
  //  createItem.into[Item].transform

  implicit class BrandOps(param: BrandParam) {
    def asBrand: Brand = param.value.value.coerce[Brand]
  }

}
