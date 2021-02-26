package shop.domain

import shop.domain.auth.UserId
import shop.domain.checkout.Card

import derevo.circe.magnolia.encoder
import derevo.derive
import squants.market.Money

object payment {

  @derive(encoder)
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )

}
