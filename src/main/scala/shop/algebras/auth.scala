package shop.algebras

import cats.MonadError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.circe.syntax._
import io.circe.parser.decode
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import pdi.jwt.JwtClaim
import shop.domain.auth._
import shop.http.auth.roles._
import shop.http.json._
import scala.util.control.NonFatal
import scala.util.Try

trait Auth[F[_]] {
  def adminJwtAuth: F[AdminJwtAuth]
  def userJwtAuth: F[UserJwtAuth]
  def findUser[A: Coercible[User, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]]
  def newUser(username: UserName, password: Password, role: AuthRole): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken): F[Unit]
}

object LiveAuth {
  def make[F[_]: Sync](
      adminToken: JwtToken,
      adminUser: AdminUser,
      adminJwtAuth: AdminJwtAuth,
      userJwtAuth: UserJwtAuth,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[Auth[F]] =
    new LiveAuth(adminToken, adminUser, adminJwtAuth, userJwtAuth, tokens, users, redis).pure[F].widen
}

class LiveAuth[F[_]: GenUUID: MonadError[?[_], Throwable]] private (
    adminToken: JwtToken,
    adminUser: AdminUser,
    adminAuth: AdminJwtAuth,
    userAuth: UserJwtAuth,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String]
) extends Auth[F] {
  import RedisKeys._

  def adminJwtAuth: F[AdminJwtAuth] = adminAuth.pure[F]
  def userJwtAuth: F[UserJwtAuth]   = userAuth.pure[F]

  private def retrieveUser[A: Coercible[User, ?]](
      token: JwtToken
  ): F[Option[A]] =
    redis
      .hGet(UsersKey.value, token.value)
      .map(_.flatMap { u =>
        decode[User](u).toOption.map(_.coerce[A])
      })

  private def findToken(token: JwtToken, field: Field): F[JwtToken] =
    redis.hGet(TokenKey.value, field.value).flatMap {
      case Some(t) if t == token.value => token.pure[F]
      case _                           => TokenNotFound.raiseError[F, JwtToken]
    }

  private def checkTokenGetUser[A: Coercible[User, ?]](
      token: JwtToken,
      field: Field
  ): F[Option[A]] =
    findToken(token, field)
      .flatMap(_ => retrieveUser[A](token))
      .recoverWith {
        case TokenNotFound => none[A].pure[F]
      }

  def findUser[A: Coercible[User, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]] =
    role match {
      case UserRole =>
        checkTokenGetUser[A](token, UserField)
      case AdminRole =>
        (token == adminToken)
          .guard[Option]
          .as(adminUser.asInstanceOf[A])
          .pure[F]
    }

  def newUser(username: UserName, password: Password, role: AuthRole): F[JwtToken] =
    role match {
      case AdminRole => UnsupportedOperation.raiseError[F, JwtToken]
      case UserRole =>
        users.find(username, password).flatMap {
          case Some(_) => UserNameInUse(username).raiseError[F, JwtToken]
          case None =>
            for {
              i <- users.create(username, password)
              t <- tokens.create
              u = User(i, username).asJson.noSpaces
              // TODO: Use args expiration
              _ <- redis.hSet(UsersKey.value, t.value, u)
              _ <- redis.hSet(TokenKey.value, t.value, t.value)
            } yield t
        }
    }

  def login(username: UserName, password: Password): F[JwtToken] =
    users.find(username, password).flatMap {
      case None => InvalidUserOrPassword(username).raiseError[F, JwtToken]
      case Some(user) =>
        tokens.create.flatTap { t =>
          // TODO: Use args expiration
          redis.hSet(UsersKey.value, t.value, user.asJson.noSpaces) *>
            redis.hSet(TokenKey.value, t.value, t.value)
        }
    }

  def logout(token: JwtToken): F[Unit] =
    redis.hDel(TokenKey.value, token.value) *>
      redis.hDel(UsersKey.value, token.value)

}

private object RedisKeys {

  sealed abstract class Key(val value: String)
  case object TokenKey extends Key("tokens")
  case object UsersKey extends Key("users")

  sealed abstract class Field(val value: String)
  case object AdminField extends Field("admin")
  case object UserField extends Field("user")

}
