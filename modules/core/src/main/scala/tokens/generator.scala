package tokens

import cats.effect._
import dev.profunktor.auth.jwt._
import io.circe.Decoder
import io.circe.parser.{ decode => jsonDecode }
import io.estatico.newtype.macros._
import pdi.jwt._

// To make it runnable, change `class` for `object`.
class TokenGenerator extends IOApp {
  import data._

  def putStrLn[A](a: A): IO[Unit] = IO(println(a))

  /* ---- Encoding stuff ---- */

  // A Claim can be any valid JSON
  val claim = JwtClaim(
    """
      {"claim": "example-claim"}
    """
  )

  // Any valid string
  val secretKey = JwtSecretKey("any-secret")

  val algo = JwtAlgorithm.HS256

  val mkToken: IO[JwtToken] =
    jwtEncode[IO](claim, secretKey, algo)

  /* ---- Decoding stuff ---- */

  val jwtAuth = JwtAuth.hmac(secretKey.value, algo)

  def decodeToken(token: JwtToken): IO[Claim] =
    jwtDecode[IO](token, jwtAuth).flatMap { c =>
      IO.fromEither(jsonDecode[Claim](c.content))
    }

  def run(args: List[String]): IO[ExitCode] =
    for {
      t <- mkToken
      _ <- putStrLn(t)
      c <- decodeToken(t)
      _ <- putStrLn(c)
    } yield ExitCode.Success

}

object data {
  @newtype case class Claim(value: String)

  object Claim {
    implicit val jsonDecoder: Decoder[Claim] =
      Decoder.forProduct1("claim")(Claim.apply)
  }
}
