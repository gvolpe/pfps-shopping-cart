package shop.domain

import io.estatico.newtype.macros.newtype
import java.{ util => ju }
import shop.http.auth.roles.UserName
import scala.util.control.NoStackTrace

object auth {

  // --------- user registration -----------

  @newtype case class NewUserName(value: String)
  @newtype case class NewPassword(value: String)

  case class CreateUser(
      username: NewUserName,
      password: NewPassword
  )

  case class UserNameInUse(username: UserName) extends NoStackTrace
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation extends NoStackTrace

  // --------- user login -----------

  @newtype case class Password(value: String)

  case class LoginUser(
      username: UserName,
      password: Password
  )
}
