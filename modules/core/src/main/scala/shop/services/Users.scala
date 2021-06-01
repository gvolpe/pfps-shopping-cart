package shop.services

import shop.domain.ID
import shop.domain.auth._
import shop.effects.GenUUID
import shop.http.auth.users._
import shop.sql.codecs._

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.implicits._

trait Users[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName, password: EncryptedPassword): F[UserId]
}

object Users {
  def make[F[_]: GenUUID: MonadCancelThrow](
      postgres: Resource[F, Session[F]]
  ): Users[F] =
    new Users[F] {
      import UserSQL._

      def find(username: UserName): F[Option[UserWithPassword]] =
        postgres.use { session =>
          session.prepare(selectUser).use { q =>
            q.option(username).map {
              case Some(u ~ p) => UserWithPassword(u.id, u.name, p).some
              case _           => none[UserWithPassword]
            }
          }
        }

      def create(username: UserName, password: EncryptedPassword): F[UserId] =
        postgres.use { session =>
          session.prepare(insertUser).use { cmd =>
            ID.make[F, UserId].flatMap { id =>
              cmd
                .execute(User(id, username) ~ password)
                .as(id)
                .recoverWith {
                  case SqlState.UniqueViolation(_) =>
                    UserNameInUse(username).raiseError[F, UserId]
                }
            }
          }
        }
    }

}

private object UserSQL {

  val codec: Codec[User ~ EncryptedPassword] =
    (userId ~ userName ~ encPassword).imap {
      case i ~ n ~ p =>
        User(i, n) ~ p
    } {
      case u ~ p =>
        u.id ~ u.name ~ p
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
        SELECT * FROM users
        WHERE name = $userName
       """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
        INSERT INTO users
        VALUES ($codec)
        """.command

}
