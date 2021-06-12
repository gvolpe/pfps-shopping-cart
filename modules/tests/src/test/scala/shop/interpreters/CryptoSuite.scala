package shop.interpreters

import shop.auth.Crypto
import shop.config.types.PasswordSalt
import shop.domain.auth.Password

import cats.effect.IO
import eu.timepit.refined.auto._
import weaver.SimpleIOSuite

object CryptoSuite extends SimpleIOSuite {

  private val salt = PasswordSalt("53kr3t")

  test("password encoding and decoding roundtrip") {
    Crypto.make[IO](salt).map { crypto =>
      val ini = Password("simple123")
      val enc = crypto.encrypt(ini)
      val dec = crypto.decrypt(enc)
      expect.same(dec, ini)
    }
  }

}
