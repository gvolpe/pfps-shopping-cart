package shop.services

import shop.database.codecs._
import shop.domain.ID
import shop.domain.brand._
import shop.effects.GenUUID

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}

object Brands {
  def make[F[_]: BracketThrow: GenUUID](
      pool: Resource[F, Session[F]]
  ): Brands[F] =
    new Brands[F] {
      import BrandQueries._

      def findAll: F[List[Brand]] =
        pool.use(_.execute(selectAll))

      def create(name: BrandName): F[Unit] =
        pool.use { session =>
          session.prepare(insertBrand).use { cmd =>
            ID.make[F, BrandId].flatMap { id =>
              cmd.execute(Brand(id, name)).void
            }
          }
        }
    }
}

private object BrandQueries {

  val codec: Codec[Brand] =
    (brandId ~ brandName).imap {
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
