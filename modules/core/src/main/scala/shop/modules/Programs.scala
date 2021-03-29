package shop.modules

import shop.config.data.CheckoutConfig
import shop.effects._
import shop.programs._

import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryPolicies._
import retry.RetryPolicy
import cats.MonadThrow
import cats.effect.Temporal

object Programs {
  def make[F[_]: Background: Logger: Sync: Temporal](
      checkoutConfig: CheckoutConfig,
      services: Services[F],
      clients: HttpClients[F]
  ): Programs[F] =
    Programs[F](checkoutConfig, services, clients)
}

final case class Programs[F[_]: Background: Logger: MonadThrow: Temporal] private (
    cfg: CheckoutConfig,
    services: Services[F],
    clients: HttpClients[F]
) {

  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](cfg.retriesLimit.value) |+| exponentialBackoff[F](cfg.retriesBackoff)

  val checkout: Checkout[F] = new Checkout[F](
    clients.payment,
    services.cart,
    services.orders,
    retryPolicy
  )

}
