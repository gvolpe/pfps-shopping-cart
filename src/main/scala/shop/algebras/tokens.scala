package shop.algebras

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.syntax._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import pdi.jwt._
import scala.concurrent.duration.FiniteDuration
import shop.config.data._

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

object LiveTokens {
  def make[F[_]: Sync](
      tokenConfig: TokenConfig,
      tokenExpiration: TokenExpiration
  ): F[Tokens[F]] =
    Sync[F].delay(java.time.Clock.systemUTC).map { implicit jClock =>
      new LiveTokens[F](tokenConfig, tokenExpiration.value)
    }
}

class LiveTokens[F[_]: GenUUID: Sync] private (
    config: TokenConfig,
    exp: FiniteDuration
)(implicit val ev: java.time.Clock)
    extends Tokens[F] {
  def create: F[JwtToken] =
    for {
      uuid <- GenUUID[F].make
      claim <- Sync[F].delay(JwtClaim(uuid.asJson.noSpaces).issuedNow.expiresIn(exp.toMillis))
      secretKey = JwtSecretKey(config.secretKey.value.value.value)
      token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
    } yield token
}
