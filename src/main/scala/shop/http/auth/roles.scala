package shop.http.auth

import dev.profunktor.auth.jwt.JwtAuth
import io.estatico.newtype.macros.newtype
import java.{ util => ju }

object roles {
  sealed abstract class AuthRole
  case object AdminRole extends AuthRole
  case object UserRole extends AuthRole

  @newtype case class AdminJwtAuth(value: JwtAuth)
  @newtype case class UserJwtAuth(value: JwtAuth)

  @newtype case class UserId(value: ju.UUID)
  @newtype case class UserName(value: String)

  case class LoggedUser(id: UserId, name: UserName)

  @newtype case class CommonUser(value: LoggedUser)
  @newtype case class AdminUser(value: LoggedUser)
}
