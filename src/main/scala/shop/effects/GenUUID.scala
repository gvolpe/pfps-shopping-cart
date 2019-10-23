package shop.algebras

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import shop.domain._
import shop.effects._
import java.{ util => ju }

trait GenUUID[F[_]] {
  def make: F[ju.UUID]
  def make[A: Coercible[ju.UUID, ?]]: F[A]
  def read[A: Coercible[ju.UUID, ?]](str: String): F[A]
}

object GenUUID {
  def apply[F[_]](implicit ev: GenUUID[F]): GenUUID[F] = ev

  implicit def syncGenUUID[F[_]: Sync]: GenUUID[F] =
    new GenUUID[F] {
      def make: F[ju.UUID] =
        Sync[F].delay(ju.UUID.randomUUID())

      def make[A: Coercible[ju.UUID, ?]]: F[A] =
        make.map(_.coerce[A])

      def read[A: Coercible[ju.UUID, ?]](str: String): F[A] =
        ApThrow[F].catchNonFatal(ju.UUID.fromString(str).coerce[A])
    }
}
