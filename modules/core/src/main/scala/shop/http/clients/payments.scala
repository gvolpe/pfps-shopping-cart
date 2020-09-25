package shop.http.clients

import cats.syntax.all._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import shop.config.data.PaymentConfig
import shop.domain.order._
import shop.domain.payment._
import shop.http.json._
import shop.effects._

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

final class LivePaymentClient[F[_]: JsonDecoder: BracketThrow](
    cfg: PaymentConfig,
    client: Client[F]
) extends PaymentClient[F]
    with Http4sClientDsl[F] {

  def process(payment: Payment): F[PaymentId] =
    Uri.fromString(cfg.uri.value.value + "/payments").liftTo[F].flatMap { uri =>
      POST(payment, uri).flatMap { req =>
        client.run(req).use { r =>
          if (r.status == Status.Ok || r.status == Status.Conflict)
            r.asJsonDecode[PaymentId]
          else
            PaymentError(
              Option(r.status.reason).getOrElse("unknown")
            ).raiseError[F, PaymentId]
        }
      }
    }

}
