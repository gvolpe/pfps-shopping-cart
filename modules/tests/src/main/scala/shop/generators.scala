package shop

import eu.timepit.refined.api.Refined
import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import java.util.UUID
import org.scalacheck.Gen
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._

object generators {

  // PostgreSQL with Skunk does not seem to accept some characters using UTF-8
  // TODO: Maybe fix Crypto impl?
  val passwordGen: Gen[Password] = {
    val values = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    Gen.nonEmptyListOf(Gen.oneOf(values)).map(_.mkString.coerce[Password])
  }

  def cbUuid[A: Coercible[UUID, ?]]: Gen[A] =
    Gen.uuid.map(_.coerce[A])

  def cbStr[A: Coercible[String, ?]]: Gen[A] =
    genNonEmptyString.map(_.coerce[A])

  def cbInt[A: Coercible[Int, ?]]: Gen[A] =
    Gen.posNum[Int].map(_.coerce[A])

  def cbBigDecimal[A: Coercible[BigDecimal, ?]]: Gen[A] =
    Gen.posNum[Long].map(n => BigDecimal(n).coerce[A])

  val genNonEmptyString: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val brandGen: Gen[Brand] =
    for {
      i <- cbUuid[BrandId]
      n <- cbStr[BrandName]
    } yield Brand(i, n)

  val categoryGen: Gen[Category] =
    for {
      i <- cbUuid[CategoryId]
      n <- cbStr[CategoryName]
    } yield Category(i, n)

  val itemGen: Gen[Item] =
    for {
      i <- cbUuid[ItemId]
      n <- cbStr[ItemName]
      d <- cbStr[ItemDescription]
      p <- cbBigDecimal[USD]
      b <- brandGen
      c <- categoryGen
    } yield Item(i, n, d, p, b, c)

  val cartItemGen: Gen[CartItem] =
    for {
      i <- itemGen
      q <- cbInt[Quantity]
    } yield CartItem(i, q)

  val cartTotalGen: Gen[CartTotal] =
    for {
      i <- Gen.nonEmptyListOf(cartItemGen)
      t <- cbBigDecimal[USD]
    } yield CartTotal(i, t)

  val itemMapGen: Gen[(ItemId, Quantity)] =
    for {
      i <- cbUuid[ItemId]
      q <- cbInt[Quantity]
    } yield i -> q

  val cartGen: Gen[Cart] =
    Gen.nonEmptyMap(itemMapGen).map(Cart.apply)

  val cardGen: Gen[Card] =
    for {
      n <- genNonEmptyString.map[CardNamePred](Refined.unsafeApply)
      u <- Gen.posNum[Long].map[CardNumberPred](Refined.unsafeApply)
      x <- Gen.posNum[Int].map[CardExpirationPred](x => Refined.unsafeApply(x.toString))
      c <- Gen.posNum[Int].map[CardCCVPred](x => Refined.unsafeApply(x.toInt))
    } yield Card(n.coerce[CardName], u.coerce[CardNumber], x.coerce[CardExpiration], c.coerce[CardCCV])

}
