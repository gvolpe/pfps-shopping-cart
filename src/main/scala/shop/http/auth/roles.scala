package shop.http.auth

import dev.profunktor.auth.jwt._
import io.estatico.newtype.macros.newtype
import shop.config.TokenExpiration
import shop.domain.auth._

object roles {
  sealed abstract class AuthRole
  case object AdminRole extends AuthRole
  case object UserRole extends AuthRole

  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  case class User(id: UserId, name: UserName)

  @newtype case class CommonUser(value: User)
  @newtype case class AdminUser(value: User)

  case class AuthData(
      adminToken: JwtToken,
      adminUser: AdminUser,
      adminJwtAuth: AdminJwtAuth,
      userJwtAuth: UserJwtAuth,
      tokenExpiration: TokenExpiration
  )

}
