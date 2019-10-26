package shop.programs

import cats.effect.IO
import shop.algebras.Orders
import shop.domain.auth.UserId
import shop.domain.cart._
import shop.domain.order._
import shop.domain.item.USD

case class TestOrders() extends Orders[IO] {
  def get(userId: UserId, orderId: OrderId): IO[Option[Order]]                                     = ???
  def findBy(userId: UserId): IO[List[Order]]                                                      = ???
  def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] = ???
}
