package shop.modules

import cats.effect._
import cats.implicits._
import org.http4s.client.Client
import shop.config.data.PaymentConfig
import shop.http.clients._

object HttpClients {
  def make[F[_]: Sync](
      cfg: PaymentConfig,
      client: Client[F]
  ): F[HttpClients[F]] =
    new LiveHttpClients[F](cfg, client).pure[F].widen
}

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}

final class LiveHttpClients[F[_]: Sync] private[modules] (
    cfg: PaymentConfig,
    client: Client[F]
) extends HttpClients[F] {
  def payment: PaymentClient[F] = new LivePaymentClient[F](cfg, client)
}
