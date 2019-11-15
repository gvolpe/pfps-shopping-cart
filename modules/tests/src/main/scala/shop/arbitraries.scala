package shop

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._
import shop.generators._

object arbitraries {

  implicit def arbCoercibleBigDecimal[A: Coercible[BigDecimal, ?]]: Arbitrary[A] =
    Arbitrary(cbBigDecimal[A])

  implicit def arbCoercibleInt[A: Coercible[Int, ?]]: Arbitrary[A] =
    Arbitrary(Gen.posNum[Int].map(_.coerce[A]))

  implicit def arbCoercibleStr[A: Coercible[String, ?]]: Arbitrary[A] =
    Arbitrary(cbStr[A])

  implicit def arbCoercibleUUID[A: Coercible[UUID, ?]]: Arbitrary[A] =
    Arbitrary(cbUuid[A])

  implicit val arbBrand: Arbitrary[Brand] =
    Arbitrary(brandGen)

  implicit val arbCategory: Arbitrary[Category] =
    Arbitrary(categoryGen)

  implicit val arbItem: Arbitrary[Item] =
    Arbitrary(itemGen)

  implicit val arbCartItem: Arbitrary[CartItem] =
    Arbitrary(cartItemGen)

  implicit val arbCartTotal: Arbitrary[CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCart: Arbitrary[Cart] =
    Arbitrary(cartGen)

  implicit val arbCard: Arbitrary[Card] =
    Arbitrary(cardGen)

}
