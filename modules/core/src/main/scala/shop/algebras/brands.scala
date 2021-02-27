package shop.algebras

import shop.domain.ID
import shop.domain.brand._
import shop.effects.GenUUID
import shop.ext.skunkx._

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}

object Brands {
  def make[F[_]: BracketThrow: GenUUID](
      sessionPool: Resource[F, Session[F]]
  ): Brands[F] =
    new Brands[F] {
      import BrandQueries._

      def findAll: F[List[Brand]] =
        sessionPool.use(_.execute(selectAll))

      def create(name: BrandName): F[Unit] =
        sessionPool.use { session =>
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
