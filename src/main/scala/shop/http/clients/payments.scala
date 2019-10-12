package shop.http.clients

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s.Uri
import org.http4s.client._
import shop.domain.cart.Cart
import shop.domain.order.OrderId
import shop.http.auth.roles.UserId
import java.{ util => ju }

trait PaymentsClient[F[_]] {
  def process(userId: UserId, cart: Cart): F[OrderId]
}

class LivePaymentsClient[F[_]: Sync](client: Client[F]) extends PaymentsClient[F] {
  def process(userId: UserId, cart: Cart): F[OrderId] = {
    // FIXME: hardcoded order id and side-effectful
    val oid = ju.UUID.randomUUID().coerce[OrderId]
    client.expect[String](Uri.unsafeFromString("http://google.com")).as(oid)
  }
}
