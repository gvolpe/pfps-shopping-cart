package shop.interpreters

import shop.algebras.LiveCrypto
import shop.config.data.PasswordSalt
import shop.domain.auth.Password

import cats.effect.IO
import ciris.Secret
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import suite._

final class CryptoSuite extends PureTestSuite {

  private val salt = PasswordSalt(Secret("53kr3t"))

  test("password encoding and decoding roundtrip") {
    IOAssertion {
      LiveCrypto.make[IO](salt).map { crypto =>
        val ini = Password("simple123")
        val enc = crypto.encrypt(ini)
        val dec = crypto.decrypt(enc)
        assertEquals(dec, ini)
      }
    }
  }

}
