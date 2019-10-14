package shop.http.clients

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s.Uri
import org.http4s.client._
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.domain.checkout.Card
import shop.domain.item.USD
import shop.domain.order.PaymentId
import java.{ util => ju }

trait PaymentClient[F[_]] {
  def process(userId: UserId, total: USD, card: Card): F[PaymentId]
}

class LivePaymentClient[F[_]: Sync](client: Client[F]) extends PaymentClient[F] {

  def process(userId: UserId, total: USD, card: Card): F[PaymentId] = {
    // FIXME: hardcoded and side-effectful payment id
    val oid = ju.UUID.randomUUID().coerce[PaymentId]
    client.expect[String](Uri.unsafeFromString("http://google.com")).as(oid)
  }

}
