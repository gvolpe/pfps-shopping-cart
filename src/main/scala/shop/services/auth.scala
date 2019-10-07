package shop.services

import cats.MonadError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{ util => ju }
import pdi.jwt.JwtClaim
import shop.domain.auth.{ Email, Password }
import shop.http.auth.roles._
import scala.util.control.NonFatal
import scala.util.Try

// TODO: We could use Neo4j to store the User-HAS_ROLE-AdminRole relationships
trait AuthService[F[_]] {
  def findUser[A: Coercible[LoggedUser, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]]
  def newUser(user: LoggedUser, role: AuthRole): F[Unit]
  def loginByUsername(username: UserName, password: Password): F[Option[JwtToken]]
  def loginByEmail(email: Email, password: Password): F[Option[JwtToken]]
  def logout(token: JwtToken): F[Unit]
}

// TODO: Use Redis to store tokens with expiration
object LiveAuthService {
  // Hardcoded Admin UUID for now
  private val adminId = ju.UUID.fromString("004b4457-71c3-4439-a1b2-03820263b59c").coerce[UserId]
  private val admin   = LoggedUser(adminId, "admin".coerce[UserName])

  def make[F[_]: Sync]: F[AuthService[F]] =
    for {
      adminRef <- Ref.of[F, Map[String, LoggedUser]](Map(admin.id.value.toString -> admin))
      usersRef <- Ref.of[F, Map[String, LoggedUser]](Map.empty)
    } yield new LiveAuthService(adminRef, usersRef)
}

class LiveAuthService[F[_]: MonadError[?[_], Throwable]] private (
    adminRef: Ref[F, Map[String, LoggedUser]],
    usersRef: Ref[F, Map[String, LoggedUser]]
) extends AuthService[F] {

  // FIXME: Should also take a JwtToken to verify against Redis. JwtClaim is not needed in this case.
  def findUser[A: Coercible[LoggedUser, ?]](role: AuthRole)(token: JwtToken)(claim: JwtClaim): F[Option[A]] = {
    println(s">>>>>>>> CONTENT ${claim.content}")
    Try(ju.UUID.fromString(claim.content.drop(1).dropRight(1)))
      .liftTo[F]
      .flatMap { uuid =>
        println(s">>>>>>>>>>>> UUID : $uuid")
        val st = role match {
          case AdminRole => adminRef.get
          case UserRole  => usersRef.get
        }
        st.map(_.get(uuid.toString).fold(none[A])(_.coerce[A].some))
      }
      .handleErrorWith {
        case NonFatal(e) => println(s">>>>>>> ${claim.content}"); e.printStackTrace(); throw e
      }
  }

  def newUser(user: LoggedUser, role: AuthRole): F[Unit] =
    role match {
      case AdminRole => adminRef.update(_.updated(user.id.value.toString, user))
      case UserRole  => usersRef.update(_.updated(user.id.value.toString, user))
    }

  def loginByUsername(username: UserName, password: Password): F[Option[JwtToken]] =
    none[JwtToken].pure[F]

  def loginByEmail(email: Email, password: Password): F[Option[JwtToken]] =
    none[JwtToken].pure[F]

  def logout(token: JwtToken): F[Unit] = ().pure[F]

}
