package shop.modules

import shop.config.data.PaymentConfig
import shop.http.clients._

import cats.effect._
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client

object HttpClients {
  def make[F[_]: BracketThrow: JsonDecoder](
      cfg: PaymentConfig,
      client: Client[F]
  ): HttpClients[F] =
    new HttpClients[F] {
      def payment: PaymentClient[F] = PaymentClient.make[F](cfg, client)
    }
}

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}
