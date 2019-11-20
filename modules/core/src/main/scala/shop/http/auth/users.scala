package shop.http.auth

import dev.profunktor.auth.jwt._
import io.estatico.newtype.macros.newtype
import shop.config.data.TokenExpiration
import shop.domain.auth._

object users {

  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  case class User(id: UserId, name: UserName)

  @newtype case class CommonUser(value: User)
  @newtype case class AdminUser(value: User)

}
