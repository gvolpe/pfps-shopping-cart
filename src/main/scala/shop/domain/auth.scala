package shop.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import java.{ util => ju }
import scala.util.control.NoStackTrace

object auth {

  @newtype case class UserId(value: ju.UUID)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  // --------- user registration -----------

  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = value.value.coerce[UserName]
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = value.value.coerce[Password]
  }

  case class CreateUser(
      username: UserNameParam,
      password: PasswordParam
  )

  case class UserNameInUse(username: UserName) extends NoStackTrace
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation extends NoStackTrace

  case object TokenNotFound extends NoStackTrace

  // --------- user login -----------

  case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )
}
