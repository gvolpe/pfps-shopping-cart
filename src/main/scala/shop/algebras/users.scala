package shop.algebras

import cats.effect.Resource
import cats.implicits._
import io.estatico.newtype.ops._
import java.{ util => ju }
import shop.domain.auth._
import shop.effects._
import shop.http.auth.roles._
import shop.ext.skunkx._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Users[F[_]] {
  def find(username: UserName, password: Password): F[Option[User]]
  def create(username: UserName, password: Password): F[UserId]
}

object LiveUsers {
  def make[F[_]: BracketThrow: GenUUID](
      sessionPool: Resource[F, Session[F]],
      crypto: Crypto
  ): F[Users[F]] =
    new LiveUsers[F](sessionPool, crypto).pure[F].widen
}

class LiveUsers[F[_]: BracketThrow: GenUUID] private (
    sessionPool: Resource[F, Session[F]],
    crypto: Crypto
) extends Users[F] {
  import UserQueries._

  def find(username: UserName, password: Password): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(selectUser).use { q =>
        q.option(username).map {
          case Some(u ~ p) if p.value == crypto.encrypt(password).value => u.some
          case _                                                        => none[User]
        }
      }
    }

  def create(username: UserName, password: Password): F[UserId] =
    sessionPool.use { session =>
      session.prepare(insertUser).use { cmd =>
        GenUUID[F].make[UserId].flatMap { id =>
          cmd
            .execute(User(id, username) ~ crypto.encrypt(password))
            .as(id)
            .handleErrorWith {
              case SqlState.UniqueViolation(_) =>
                UserNameInUse(username).raiseError[F, UserId]
            }
        }
      }
    }

}

private object UserQueries {

  val codec: Codec[User ~ EncryptedPassword] =
    (varchar ~ varchar ~ varchar).imap {
      case i ~ n ~ p =>
        User(
          ju.UUID.fromString(i).coerce[UserId],
          n.coerce[UserName]
        ) ~ p.coerce[EncryptedPassword]
    } {
      case u ~ p =>
        u.id.value.toString ~ u.name.value ~ p.value
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
        SELECT * FROM users
        WHERE name = ${coercibleVarchar[UserName]}
       """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
        INSERT INTO users
        VALUES ($codec)
        """.command

}
