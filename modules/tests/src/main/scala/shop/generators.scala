package shop

import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._

import eu.timepit.refined.api.Refined
import org.scalacheck.Gen
import squants.market._

object generators {

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val brandIdGen: Gen[BrandId] =
    Gen.uuid.map(BrandId(_))

  val brandNameGen: Gen[BrandName] =
    nonEmptyStringGen.map(BrandName(_))

  val categoryIdGen: Gen[CategoryId] =
    Gen.uuid.map(CategoryId(_))

  val categoryNameGen: Gen[CategoryName] =
    nonEmptyStringGen.map(CategoryName(_))

  val itemIdGen: Gen[ItemId] =
    Gen.uuid.map(ItemId(_))

  val itemNameGen: Gen[ItemName] =
    nonEmptyStringGen.map(ItemName(_))

  val itemDescriptionGen: Gen[ItemDescription] =
    nonEmptyStringGen.map(ItemDescription(_))

  val userIdGen: Gen[UserId] =
    Gen.uuid.map(UserId(_))

  val orderIdGen: Gen[OrderId] =
    Gen.uuid.map(OrderId(_))

  val paymentIdGen: Gen[PaymentId] =
    Gen.uuid.map(PaymentId(_))

  val userNameGen: Gen[UserName] =
    nonEmptyStringGen.map(UserName(_))

  val passwordGen: Gen[Password] =
    nonEmptyStringGen.map(Password(_))

  val quantityGen: Gen[Quantity] =
    Gen.posNum[Int].map(Quantity(_))

  val moneyGen: Gen[Money] =
    Gen.posNum[Long].map(n => USD(BigDecimal(n)))

  val brandGen: Gen[Brand] =
    for {
      i <- brandIdGen
      n <- brandNameGen
    } yield Brand(i, n)

  val categoryGen: Gen[Category] =
    for {
      i <- categoryIdGen
      n <- categoryNameGen
    } yield Category(i, n)

  val itemGen: Gen[Item] =
    for {
      i <- itemIdGen
      n <- itemNameGen
      d <- itemDescriptionGen
      p <- moneyGen
      b <- brandGen
      c <- categoryGen
    } yield Item(i, n, d, p, b, c)

  val cartItemGen: Gen[CartItem] =
    for {
      i <- itemGen
      q <- quantityGen
    } yield CartItem(i, q)

  val cartTotalGen: Gen[CartTotal] =
    for {
      i <- Gen.nonEmptyListOf(cartItemGen)
      t <- moneyGen
    } yield CartTotal(i, t)

  val itemMapGen: Gen[(ItemId, Quantity)] =
    for {
      i <- itemIdGen
      q <- quantityGen
    } yield i -> q

  val cartGen: Gen[Cart] =
    Gen.nonEmptyMap(itemMapGen).map(Cart.apply)

  val cardGen: Gen[Card] =
    for {
      n <- nonEmptyStringGen.map[CardNamePred](Refined.unsafeApply)
      u <- Gen.posNum[Long].map[CardNumberPred](Refined.unsafeApply)
      x <- Gen.posNum[Int].map[CardExpirationPred](x => Refined.unsafeApply(x.toString))
      c <- Gen.posNum[Int].map[CardCVVPred](Refined.unsafeApply)
    } yield Card(CardName(n), CardNumber(u), CardExpiration(x), CardCVV(c))

}
