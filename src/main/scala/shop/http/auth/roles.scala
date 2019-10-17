package shop.http.auth

import dev.profunktor.auth.jwt.JwtAuth
import io.estatico.newtype.macros.newtype
import shop.domain.auth._

object roles {
  sealed abstract class AuthRole
  case object AdminRole extends AuthRole
  case object UserRole extends AuthRole

  @newtype case class AdminJwtAuth(value: JwtAuth)
  @newtype case class UserJwtAuth(value: JwtAuth)

  case class User(id: UserId, name: UserName)

  @newtype case class CommonUser(value: User)
  @newtype case class AdminUser(value: User)
}
