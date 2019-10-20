package shop.algebras

import cats.effect._
import cats.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import io.estatico.newtype.ops._
import shop.database._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.item._
import shop.domain.order._
import shop.http.json._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import java.{ util => ju }

trait Orders[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[List[Order]]
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: USD
  ): F[OrderId]
}

object LiveOrders {
  def make[F[_]: Sync](session: Session[F]): F[Orders[F]] =
    new LiveOrders[F](session).pure[F].widen
}

private class LiveOrders[F[_]: Sync](
    session: Session[F]
) extends Orders[F] {
  import OrderQueries._

  def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    session.prepare(selectByUserIdAndOrderId).use { q =>
      q.option(userId ~ orderId)
    }

  def findBy(userId: UserId): F[List[Order]] =
    session.prepare(selectByUserId).use { q =>
      q.stream(userId, 1024).compile.toList
    }

  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: USD
  ): F[OrderId] =
    session.prepare(insertOrder).use { cmd =>
      GenUUID[F].make[OrderId].flatMap { id =>
        val itMap = items.map(x => x.item.uuid -> x.quantity).toMap
        val order = Order(id, paymentId, itMap, total)
        cmd.execute(userId ~ order).as(id)
      }
    }

}

private object OrderQueries {

  val decoder: Decoder[Order] =
    (varchar ~ varchar ~ varchar ~ varchar ~ numeric).map {
      case o ~ _ ~ p ~ i ~ t =>
        Order(
          ju.UUID.fromString(o).coerce[OrderId],
          ju.UUID.fromString(p).coerce[PaymentId],
          decode[Map[ItemId, Quantity]](i).getOrElse(Map.empty),
          t.coerce[USD]
        )
    }

  val selectByUserId: Query[UserId, Order] =
    sql"""
        SELECT * FROM orders
        WHERE user_id = ${coercibleUuid[UserId]}
       """.query(decoder)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
    sql"""
        SELECT * FROM orders
        WHERE user_id = ${coercibleUuid[UserId]}
        AND order_id = ${coercibleUuid[OrderId]}
       """.query(decoder)

  val insertOrder: Command[UserId ~ Order] =
    sql"""
        INSERT INTO orders
        VALUES ($varchar, $varchar, $varchar, $varchar, $numeric)
       """.command.contramap {
      case id ~ o =>
        o.id.value.toString ~ id.value.toString ~ o.paymentId.value.toString ~ o.items.asJson.noSpaces ~ o.total.value
    }

}
