package shop.algebras

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.syntax._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import pdi.jwt._
import shop.config.TokenConfig
import LiveTokens.JwtExpiration

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

object LiveTokens {
  @newtype case class JwtExpiration(value: Long)

  val OneHour = (60L * 60).coerce[JwtExpiration]

  def make[F[_]: Sync](tokenConfig: TokenConfig): F[Tokens[F]] =
    Sync[F].delay(java.time.Clock.systemUTC).map { implicit jClock =>
      new LiveTokens[F](tokenConfig, OneHour)
    }
}

class LiveTokens[F[_]: GenUUID: Sync] private (
    config: TokenConfig,
    exp: JwtExpiration
)(implicit val ev: java.time.Clock)
    extends Tokens[F] {
  def create: F[JwtToken] =
    for {
      uuid <- GenUUID[F].make
      claim <- Sync[F].delay(JwtClaim(uuid.asJson.noSpaces).issuedNow.expiresIn(exp.value))
      secretKey = JwtSecretKey(config.secretKey.value.value.value)
      token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
    } yield token
}
