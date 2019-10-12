package shop.modules

import cats.effect._
import cats.implicits._
import org.http4s.client.Client
import shop.http.clients._

object HttpClients {
  def make[F[_]: Sync](client: Client[F]): F[HttpClients[F]] =
    new LiveHttpClients[F](client).pure[F].widen
}

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}

class LiveHttpClients[F[_]: Sync] private[modules] (
    client: Client[F]
) extends HttpClients[F] {
  def payment: PaymentClient[F] = new LivePaymentClient[F](client)
}
