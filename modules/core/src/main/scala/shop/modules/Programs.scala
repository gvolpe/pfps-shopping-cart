package shop.modules

import shop.config.data.CheckoutConfig
import shop.effects._
import shop.programs._

import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import retry.RetryPolicies._
import retry.RetryPolicy

object Programs {
  def make[F[_]: Background: Logger: Sync: Timer](
      checkoutConfig: CheckoutConfig,
      algebras: Algebras[F],
      clients: HttpClients[F]
  ): Programs[F] =
    Programs[F](checkoutConfig, algebras, clients)
}

final case class Programs[F[_]: Background: Logger: MonadThrow: Timer] private (
    cfg: CheckoutConfig,
    algebras: Algebras[F],
    clients: HttpClients[F]
) {

  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](cfg.retriesLimit.value) |+| exponentialBackoff[F](cfg.retriesBackoff)

  val checkout: CheckoutProgram[F] = new CheckoutProgram[F](
    clients.payment,
    algebras.cart,
    algebras.orders,
    retryPolicy
  )

}
