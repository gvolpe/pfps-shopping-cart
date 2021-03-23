package shop.sql

import shop.domain.auth._
import shop.domain.brand._
import shop.domain.category._
import shop.domain.item._
import shop.domain.order._

import skunk._
import skunk.codec.all._
import squants.market._

object codecs {
  val brandId: Codec[BrandId]     = uuid.imap[BrandId](BrandId(_))(_.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](BrandName(_))(_.value)

  val categoryId: Codec[CategoryId]     = uuid.imap[CategoryId](CategoryId(_))(_.value)
  val categoryName: Codec[CategoryName] = varchar.imap[CategoryName](CategoryName(_))(_.value)

  val itemId: Codec[ItemId]            = uuid.imap[ItemId](ItemId(_))(_.value)
  val itemName: Codec[ItemName]        = varchar.imap[ItemName](ItemName(_))(_.value)
  val itemDesc: Codec[ItemDescription] = varchar.imap[ItemDescription](ItemDescription(_))(_.value)

  val orderId: Codec[OrderId]     = uuid.imap[OrderId](OrderId(_))(_.value)
  val paymentId: Codec[PaymentId] = uuid.imap[PaymentId](PaymentId(_))(_.value)

  val userId: Codec[UserId]     = uuid.imap[UserId](UserId(_))(_.value)
  val userName: Codec[UserName] = varchar.imap[UserName](UserName(_))(_.value)

  val money: Codec[Money] = numeric.imap[Money](USD(_))(_.amount)

  val encPassword: Codec[EncryptedPassword] = varchar.imap[EncryptedPassword](EncryptedPassword(_))(_.value)
}
