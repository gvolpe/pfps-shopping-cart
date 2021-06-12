package shop.modules

import shop.config.types.CheckoutConfig
import shop.effects._
import shop.programs._

import cats.effect.Temporal
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryPolicies._
import retry.RetryPolicy

object Programs {
  def make[F[_]: Background: Logger: Temporal](
      checkoutConfig: CheckoutConfig,
      services: Services[F],
      clients: HttpClients[F]
  ): Programs[F] =
    new Programs[F](checkoutConfig, services, clients) {}
}

sealed abstract class Programs[F[_]: Background: Logger: Temporal] private (
    cfg: CheckoutConfig,
    services: Services[F],
    clients: HttpClients[F]
) {

  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](cfg.retriesLimit.value) |+| exponentialBackoff[F](cfg.retriesBackoff)

  val checkout: Checkout[F] = Checkout[F](
    clients.payment,
    services.cart,
    services.orders,
    retryPolicy
  )

}
