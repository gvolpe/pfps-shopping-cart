package shop

import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import shop.domain.brand._

object arbitraries {

  implicit val arbBrand: Arbitrary[Brand] =
    Arbitrary(brandGen)

  implicit val arbUuid: Arbitrary[UUID] =
    Arbitrary(Gen.uuid)

  private def cbUuid[A: Coercible[UUID, ?]]: Gen[A] =
    Gen.uuid.map(_.coerce[A])

  private def cbStr[A: Coercible[String, ?]]: Gen[A] =
    Gen.alphaStr.map(_.coerce[A])

  val brandGen: Gen[Brand] =
    for {
      i <- cbUuid[BrandId]
      n <- cbStr[BrandName]
    } yield Brand(i, n)

}
