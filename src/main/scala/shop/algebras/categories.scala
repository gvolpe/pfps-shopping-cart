package shop.algebras

import cats.effect.Resource
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.category._
import shop.effects._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[Unit]
}

object LiveCategories {
  def make[F[_]: BracketThrow: GenUUID](
      sessionPool: Resource[F, Session[F]]
  ): F[Categories[F]] =
    new LiveCategories[F](sessionPool).pure[F].widen
}

final class LiveCategories[F[_]: BracketThrow: GenUUID] private (
    sessionPool: Resource[F, Session[F]]
) extends Categories[F] {
  import CategoryQueries._

  def findAll: F[List[Category]] =
    sessionPool.use(_.execute(selectAll))

  def create(name: CategoryName): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertCategory).use { cmd =>
        GenUUID[F].make[CategoryId].flatMap { id =>
          cmd.execute(Category(id, name)).void
        }
      }
    }

}

private object CategoryQueries {

  val codec: Codec[Category] =
    (varchar ~ varchar).imap {
      case i ~ n =>
        Category(
          ju.UUID.fromString(i).coerce[CategoryId],
          n.coerce[CategoryName]
        )
    }(c => c.uuid.value.toString ~ c.name.value)

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
