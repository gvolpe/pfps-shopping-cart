package shop

import java.util.UUID

import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.http.auth.users._

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

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val brandIdGen: Gen[BrandId] =
    idGen(BrandId.apply)

  val brandNameGen: Gen[BrandName] =
    nesGen(BrandName.apply)

  val categoryIdGen: Gen[CategoryId] =
    idGen(CategoryId.apply)

  val categoryNameGen: Gen[CategoryName] =
    nesGen(CategoryName.apply)

  val itemIdGen: Gen[ItemId] =
    idGen(ItemId.apply)

  val itemNameGen: Gen[ItemName] =
    nesGen(ItemName.apply)

  val itemDescriptionGen: Gen[ItemDescription] =
    nesGen(ItemDescription.apply)

  val userIdGen: Gen[UserId] =
    idGen(UserId.apply)

  val orderIdGen: Gen[OrderId] =
    idGen(OrderId.apply)

  val paymentIdGen: Gen[PaymentId] =
    idGen(PaymentId.apply)

  val userNameGen: Gen[UserName] =
    nesGen(UserName.apply)

  val passwordGen: Gen[Password] =
    nesGen(Password.apply)

  val quantityGen: Gen[Quantity] =
    Gen.posNum[Int].map(Quantity.apply)

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

  // http routes generators

  val userGen: Gen[User] =
    for {
      i <- userIdGen
      n <- userNameGen
    } yield User(i, n)

  val commonUserGen: Gen[CommonUser] =
    userGen.map(CommonUser(_))

}
