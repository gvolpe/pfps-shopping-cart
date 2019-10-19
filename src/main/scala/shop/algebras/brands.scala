package shop.algebras

import cats.effect._
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.brand._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(brand: BrandName): F[Unit]
}

object LiveBrands {
  def make[F[_]: Sync](session: Session[F]): F[Brands[F]] =
    new LiveBrands[F](session).pure[F].widen
}

class LiveBrands[F[_]: Sync] private (
    session: Session[F]
) extends Brands[F] {
  import BrandQueries._

  def findAll: F[List[Brand]] =
    session.execute(selectAll)

  def create(brand: BrandName): F[Unit] =
    session.prepare(insertBrand).use { cmd =>
      GenUUID[F].make[BrandId].flatMap { id =>
        cmd.execute(Brand(id, brand)).void
      }
    }
}

private object BrandQueries {

  val brandCodec: Codec[Brand] =
    (varchar ~ varchar).imap {
      case i ~ n =>
        Brand(
          ju.UUID.fromString(i).coerce[BrandId],
          n.coerce[BrandName]
        )
    }(b => b.uuid.value.toString ~ b.name.value)

  val selectAll: Query[Void, Brand] =
    sql"""
        SELECT * FROM brands
       """.query(brandCodec)

  val insertBrand: Command[Brand] =
    sql"""
        INSERT INTO brands
        VALUES ($varchar, $varchar)
        """.command.contramap(b => b.uuid.value.toString ~ b.name.value)

}
