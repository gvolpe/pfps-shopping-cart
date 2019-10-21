package shop.algebras

import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.auth._
import shop.database._
import shop.effects._
import shop.http.auth.roles._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Users[F[_]] {
  def find(username: UserName, password: Password): F[Option[User]]
  def create(username: UserName, password: Password): F[UserId]
}

object LiveUsers {
  def make[F[_]: BracketThrow: GenUUID](
      session: Session[F]
  ): F[Users[F]] =
    new LiveUsers[F](session).pure[F].widen
}

class LiveUsers[F[_]: BracketThrow: GenUUID] private (
    session: Session[F]
) extends Users[F] {
  import UserQueries._

  def find(username: UserName, password: Password): F[Option[User]] =
    session.prepare(selectUser).use { q =>
      q.option(username).map {
        // TODO: encode password to compare
        case Some(u ~ p) if p.value == password.value => u.some
        case _                                        => none[User]
      }
    }

  def create(username: UserName, password: Password): F[UserId] =
    session.prepare(insertUser).use { cmd =>
      GenUUID[F].make[UserId].flatMap { id =>
        cmd
          .execute(User(id, username) ~ password)
          .as(id)
          .handleErrorWith {
            case SqlState.UniqueViolation(_) =>
              UserNameInUse(username).raiseError[F, UserId]
          }
      }
    }

}

private object UserQueries {

  val decoder: Decoder[User ~ Password] =
    (varchar ~ varchar ~ varchar).map {
      case i ~ n ~ p =>
        User(
          ju.UUID.fromString(i).coerce[UserId],
          n.coerce[UserName]
        ) ~ p.coerce[Password]
    }

  val selectUser: Query[UserName, User ~ Password] =
    sql"""
        SELECT * FROM users
        WHERE name = ${coercibleVarchar[UserName]}
       """.query(decoder)

  val insertUser: Command[User ~ Password] =
    sql"""
        INSERT INTO users
        VALUES ($varchar, $varchar, $varchar)
        """.command.contramap {
      case u ~ p =>
        u.id.value.toString ~ u.name.value ~ p.value
    }

}
