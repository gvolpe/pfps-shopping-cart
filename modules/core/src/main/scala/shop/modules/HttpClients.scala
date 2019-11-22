package shop.modules

import cats.effect._
import org.http4s.client.Client
import shop.config.data.PaymentConfig
import shop.http.clients._

object HttpClients {
  def make[F[_]: Sync](
      cfg: PaymentConfig,
      client: Client[F]
  ): F[HttpClients[F]] =
    Sync[F].delay(
      new HttpClients[F] {
        def payment: PaymentClient[F] = new LivePaymentClient[F](cfg, client)
      }
    )
}

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}
