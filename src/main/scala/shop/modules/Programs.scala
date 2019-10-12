package shop.modules

import cats.effect._
import cats.implicits._
import shop.programs._

object Programs {
  def make[F[_]: Sync](
      algebras: Algebras[F],
      clients: HttpClients[F]
  ): F[Programs[F]] =
    new Programs[F](algebras, clients).pure[F]
}

class Programs[F[_]: Sync] private (
    algebras: Algebras[F],
    clients: HttpClients[F]
) {

  def checkout: CheckoutProgram[F] = new CheckoutProgram[F](
    clients.payment,
    algebras.cart,
    algebras.orders
  )

}
