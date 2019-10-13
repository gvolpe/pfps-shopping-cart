package shop.modules

import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import shop.effects._
import shop.programs._

object Programs {
  def make[F[_]: Background: Logger: MonadThrow: Timer](
      algebras: Algebras[F],
      clients: HttpClients[F]
  ): F[Programs[F]] =
    new Programs[F](algebras, clients).pure[F]
}

class Programs[F[_]: Background: Logger: MonadThrow: Timer] private (
    algebras: Algebras[F],
    clients: HttpClients[F]
) {

  def checkout: CheckoutProgram[F] = new CheckoutProgram[F](
    clients.payment,
    algebras.cart,
    algebras.orders
  )

}
