package shop.algebras

import cats.Applicative
import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.order._

// TODO: Create a program with the interaction between submitting the cart to the payments remote
// service, creating the response in PostgreSQL and resetting the cart for the user.
trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(userId: UserId, paymentId: PaymentId, cart: Cart): F[OrderId]
}

object LiveOrders {
  def make[F[_]: Sync]: F[Orders[F]] =
    new LiveOrders[F].pure[F].widen
}

private class LiveOrders[F[_]: Applicative: GenUUID] extends Orders[F] {

  def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    none[Order].pure[F]

  def findBy(userId: UserId): F[List[Order]] =
    List.empty.pure[F]

  def create(userId: UserId, paymentId: PaymentId, cart: Cart): F[OrderId] =
    GenUUID[F].make.map(_.coerce[OrderId])

}

