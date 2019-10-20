package shop.http.clients

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.client._
import shop.domain.auth.UserId
import shop.domain.cart.Cart
import shop.domain.checkout.Card
import shop.domain.item.USD
import shop.domain.order._
import shop.http.json._
import java.{ util => ju }

trait PaymentClient[F[_]] {
  def process(userId: UserId, total: USD, card: Card): F[PaymentId]
}

class LivePaymentClient[F[_]: Sync](client: Client[F]) extends PaymentClient[F] {

  private val baseUri = "http://localhost:8080/api/v1"

  def process(userId: UserId, total: USD, card: Card): F[PaymentId] =
    Uri.fromString(baseUri + "/payments").liftTo[F].flatMap { uri =>
      client.get[PaymentId](uri) { r =>
        if (r.status == Status.Ok || r.status == Status.Conflict)
          r.as[PaymentId]
        else
          PaymentError(r.status.reason).raiseError[F, PaymentId]
      }
    }

}
