package shop.http.clients

import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import shop.config.data.PaymentConfig
import shop.domain.auth.UserId
import shop.domain.checkout.Card
import shop.domain.order._
import shop.effects._
import shop.http.json._
import squants.market.Money

trait PaymentClient[F[_]] {
  def process(userId: UserId, total: Money, card: Card): F[PaymentId]
}

final class LivePaymentClient[F[_]: JsonDecoder: MonadThrow](
    cfg: PaymentConfig,
    client: Client[F]
) extends PaymentClient[F] {

  def process(userId: UserId, total: Money, card: Card): F[PaymentId] =
    Uri.fromString(cfg.uri.value.value + "/payments").liftTo[F].flatMap { uri =>
      client
        .get[PaymentId](uri) { r =>
          if (r.status == Status.Ok || r.status == Status.Conflict)
            r.asJsonDecode[PaymentId]
          else
            PaymentError(r.status.reason).raiseError[F, PaymentId]
        }
    }

}
