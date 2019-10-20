package shop.algebras

import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.brand._
import shop.effects._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}

object LiveBrands {
  def make[F[_]: BracketThrow: GenUUID](
      session: Session[F]
  ): F[Brands[F]] =
    new LiveBrands[F](session).pure[F].widen
}

class LiveBrands[F[_]: BracketThrow: GenUUID] private (
    session: Session[F]
) extends Brands[F] {
  import BrandQueries._

  def findAll: F[List[Brand]] =
    session.execute(selectAll)

  def create(name: BrandName): F[Unit] =
    session.prepare(insertBrand).use { cmd =>
      GenUUID[F].make[BrandId].flatMap { id =>
        cmd.execute(Brand(id, name)).void
      }
    }
}

private object BrandQueries {

  val decoder: Decoder[Brand] =
    (varchar ~ varchar).map {
      case i ~ n =>
        Brand(
          ju.UUID.fromString(i).coerce[BrandId],
          n.coerce[BrandName]
        )
    }

  val encoder: Brand => String ~ String =
    b => b.uuid.value.toString ~ b.name.value

  val selectAll: Query[Void, Brand] =
    sql"""
        SELECT * FROM brands
       """.query(decoder)

  val insertBrand: Command[Brand] =
    sql"""
        INSERT INTO brands
        VALUES ($varchar, $varchar)
        """.command.contramap(encoder)

}
