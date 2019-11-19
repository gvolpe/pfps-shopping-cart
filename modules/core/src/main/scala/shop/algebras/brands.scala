package shop.algebras

import cats.effect._
import cats.implicits._
import shop.domain.brand._
import shop.effects._
import shop.ext.skunkx._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}

object LiveBrands {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Brands[F]] =
    Sync[F].delay(
      new LiveBrands[F](sessionPool)
    )
}

final class LiveBrands[F[_]: BracketThrow: GenUUID] private (
    sessionPool: Resource[F, Session[F]]
) extends Brands[F] {
  import BrandQueries._

  def findAll: F[List[Brand]] =
    sessionPool.use(_.execute(selectAll))

  def create(name: BrandName): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertBrand).use { cmd =>
        GenUUID[F].make[BrandId].flatMap { id =>
          cmd.execute(Brand(id, name)).void
        }
      }
    }
}

private object BrandQueries {

  val codec: Codec[Brand] =
    (uuid.cimap[BrandId] ~ varchar.cimap[BrandName]).imap {
      case i ~ n => Brand(i, n)
    }(b => b.uuid ~ b.name)

  val selectAll: Query[Void, Brand] =
    sql"""
        SELECT * FROM brands
       """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
        INSERT INTO brands
        VALUES ($codec)
        """.command

}
