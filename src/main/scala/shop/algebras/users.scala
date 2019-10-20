package shop.algebras

import cats.implicits._
import shop.domain.auth._
import shop.effects._
import shop.http.auth.roles._

trait Users[F[_]] {
  def find(username: UserName, password: Password): F[Option[User]]
  def create(username: UserName, password: Password): F[UserId]
}

// TODO: Use PostgreSQL
class LiveUsers[F[_]: ApThrow] extends Users[F] {

  def find(username: UserName, password: Password): F[Option[User]] =
    none[User].pure[F]

  def create(username: UserName, password: Password): F[UserId] =
    new Exception("").raiseError[F, UserId]

}
