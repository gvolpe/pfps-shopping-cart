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
import shop.effects._
import shop.http.auth.roles._
import shop.http.json._

trait Auth[F[_]] {
  def adminJwtAuth: F[AdminJwtAuth]
  def userJwtAuth: F[UserJwtAuth]
  def findUser[A: Coercible[User, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]]
  def newUser(username: UserName, password: Password, role: AuthRole): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}

object LiveAuth {
  def make[F[_]: Sync](
      authData: AuthData,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[Auth[F]] =
    new LiveAuth(authData, tokens, users, redis).pure[F].widen
}

final class LiveAuth[F[_]: GenUUID: MonadThrow] private (
    authData: AuthData,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String]
) extends Auth[F] {

  private val TokenExpiration = authData.tokenExpiration.value

  def adminJwtAuth: F[AdminJwtAuth] = authData.adminJwtAuth.pure[F]
  def userJwtAuth: F[UserJwtAuth]   = authData.userJwtAuth.pure[F]

  private def findUserByToken[A: Coercible[User, ?]](
      token: JwtToken
  ): F[Option[A]] =
    redis
      .get(token.value)
      .map(_.flatMap { u =>
        decode[User](u).toOption.map(_.coerce[A])
      })

  def findUser[A: Coercible[User, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]] =
    role match {
      case UserRole =>
        findUserByToken[A](token)
      case AdminRole =>
        (token == authData.adminToken)
          .guard[Option]
          .as(authData.adminUser.value.coerce[A])
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
              _ <- redis.setEx(t.value, u, TokenExpiration)
              _ <- redis.setEx(username.value, t.value, TokenExpiration)
            } yield t
        }
    }

  def login(username: UserName, password: Password): F[JwtToken] =
    users.find(username, password).flatMap {
      case None => InvalidUserOrPassword(username).raiseError[F, JwtToken]
      case Some(user) =>
        redis.get(username.value).flatMap {
          case Some(t) => JwtToken(t).pure[F]
          case None =>
            tokens.create.flatTap { t =>
              redis.setEx(t.value, user.asJson.noSpaces, TokenExpiration) *>
                redis.setEx(username.value, t.value, TokenExpiration)
            }
        }
    }

  def logout(token: JwtToken, username: UserName): F[Unit] =
    redis.del(token.value) *> redis.del(username.value)

}
