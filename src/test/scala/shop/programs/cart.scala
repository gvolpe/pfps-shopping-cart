package shop.programs

import cats.effect.IO
import shop.algebras.ShoppingCart
import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.item.ItemId

case class TestCart() extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
  def get(userId: UserId): IO[CartTotal]                                = ???
  def delete(userId: UserId): IO[Unit]                                  = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = ???
  def update(userId: UserId, cart: Cart): IO[Unit]                      = ???
}
