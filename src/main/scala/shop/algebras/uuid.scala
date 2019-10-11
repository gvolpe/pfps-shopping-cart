package shop.algebras

import cats.Applicative
import cats.effect.Sync
import shop.domain._
import java.{ util => ju }

trait GenUUID[F[_]] {
  def make: F[ju.UUID]
}

object GenUUID {
  def apply[F[_]](implicit ev: GenUUID[F]): GenUUID[F] = ev

  implicit def syncGenUUID[F[_]: Sync]: GenUUID[F] =
    new GenUUID[F] {
      def make: F[ju.UUID] =
        Sync[F].delay(ju.UUID.randomUUID())
    }
}
