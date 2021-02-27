package shop.algebras

import shop.domain.ID
import shop.domain.category._
import shop.effects.GenUUID
import shop.ext.skunkx._

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[Unit]
}

object Categories {
  def make[F[_]: BracketThrow: GenUUID](
      sessionPool: Resource[F, Session[F]]
  ): Categories[F] =
    new Categories[F] {
      import CategoryQueries._

      def findAll: F[List[Category]] =
        sessionPool.use(_.execute(selectAll))

      def create(name: CategoryName): F[Unit] =
        sessionPool.use { session =>
          session.prepare(insertCategory).use { cmd =>
            ID.make[F, CategoryId].flatMap { id =>
              cmd.execute(Category(id, name)).void
            }
          }
        }
    }
}

private object CategoryQueries {

  val codec: Codec[Category] =
    (uuid.cimap[CategoryId] ~ varchar.cimap[CategoryName]).imap {
      case i ~ n => Category(i, n)
    }(c => c.uuid ~ c.name)

  val selectAll: Query[Void, Category] =
    sql"""
        SELECT * FROM categories
       """.query(codec)

  val insertCategory: Command[Category] =
    sql"""
        INSERT INTO categories
        VALUES ($codec)
        """.command

}
