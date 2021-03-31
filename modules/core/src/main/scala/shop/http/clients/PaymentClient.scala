package shop.http.clients

import shop.config.data.PaymentConfig
import shop.domain.order._
import shop.domain.payment._

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

object PaymentClient {
  def make[F[_]: JsonDecoder: MonadCancelThrow](
      cfg: PaymentConfig,
      client: Client[F]
  ): PaymentClient[F] =
    new PaymentClient[F] with Http4sClientDsl[F] {
      def process(payment: Payment): F[PaymentId] =
        Uri.fromString(cfg.uri.value + "/payments").liftTo[F].flatMap { uri =>
          client.run(POST(payment, uri)).use { r =>
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
