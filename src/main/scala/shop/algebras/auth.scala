package shop.algebras

import cats.MonadError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import pdi.jwt.JwtClaim
import shop.domain.auth._
import shop.http.auth.roles._
import scala.util.control.NonFatal
import scala.util.Try

trait Auth[F[_]] {
  def adminJwtAuth: F[AdminJwtAuth]
  def userJwtAuth: F[UserJwtAuth]
  def findUser[A: Coercible[LoggedUser, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]]
  def newUser(username: UserName, password: Password, role: AuthRole): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken): F[Unit]
}

// TODO: Use Redis to store tokens with expiration
object LiveAuth {
  def make[F[_]: Sync](
      adminToken: JwtToken,
      adminUser: AdminUser,
      adminJwtAuth: AdminJwtAuth,
      userJwtAuth: UserJwtAuth,
      tokens: Tokens[F]
  ): F[Auth[F]] =
    for {
      adminTokens <- Ref.of[F, Map[JwtToken, LoggedUser]](Map(adminToken -> adminUser.coerce[LoggedUser]))
      userTokens <- Ref.of[F, Map[JwtToken, LoggedUser]](Map.empty)
      usersRef <- Ref.of[F, Map[UserName, (LoggedUser, Password)]](Map.empty)
    } yield new LiveAuth(adminTokens, userTokens, usersRef, adminJwtAuth, userJwtAuth, tokens)
}

class LiveAuth[F[_]: GenUUID: MonadError[?[_], Throwable]] private (
    adminTokens: Ref[F, Map[JwtToken, LoggedUser]],
    userTokens: Ref[F, Map[JwtToken, LoggedUser]],
    users: Ref[F, Map[UserName, (LoggedUser, Password)]],
    adminAuth: AdminJwtAuth,
    userAuth: UserJwtAuth,
    tokens: Tokens[F]
) extends Auth[F] {

  def adminJwtAuth: F[AdminJwtAuth] = adminAuth.pure[F]
  def userJwtAuth: F[UserJwtAuth]   = userAuth.pure[F]

  // FIXME: Should also take a JwtToken to verify against Redis. JwtClaim is not needed in this case.
  // Maybe for extra security we can persist the JwtClaim to compare against.
  def findUser[A: Coercible[LoggedUser, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]] =
    role match {
      case AdminRole => adminTokens.get.map(_.get(token).asInstanceOf[Option[A]])
      case UserRole  => userTokens.get.map(_.get(token).asInstanceOf[Option[A]])
    }

  def newUser(username: UserName, password: Password, role: AuthRole): F[JwtToken] =
    role match {
      case AdminRole => UnsupportedOperation.raiseError[F, JwtToken]
      case UserRole =>
        users.get.flatMap {
          _.get(username) match {
            case None =>
              GenUUID[F].make.flatMap { uuid =>
                val user = LoggedUser(uuid.coerce[UserId], username)
                users.update(_.updated(username, user -> password)) *> login(username, password)
              }
            case Some(_) => UserNameInUse(username).raiseError[F, JwtToken]
          }
        }
    }

  def login(username: UserName, password: Password): F[JwtToken] =
    users.get.flatMap {
      _.get(username) match {
        // TODO: Encrypt passwords
        case Some((u, p)) if p == password =>
          tokens.create.flatTap { token =>
            userTokens.update(_.updated(token, u))
          }
        case _ => InvalidUserOrPassword(username).raiseError[F, JwtToken]
      }
    }

  def logout(token: JwtToken): F[Unit] =
    userTokens.update(_.removed(token))

}
