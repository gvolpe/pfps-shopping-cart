package shop.domain

import io.estatico.newtype.macros.newtype
import java.{ util => ju }
import scala.util.control.NoStackTrace

object auth {

  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  // --------- user registration -----------

  case class CreateUser(
      username: UserName,
      password: Password
  )

  case class UserNameInUse(username: UserName) extends NoStackTrace
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation extends NoStackTrace

  // --------- user login -----------

  case class LoginUser(
      username: UserName,
      password: Password
  )
}
