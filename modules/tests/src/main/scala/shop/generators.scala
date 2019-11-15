package shop

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import java.util.UUID
import org.scalacheck.Gen
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._

object generators {

  def cbUuid[A: Coercible[UUID, ?]]: Gen[A] =
    Gen.uuid.map(_.coerce[A])

  def cbStr[A: Coercible[String, ?]]: Gen[A] =
    Gen.alphaStr.suchThat(_.nonEmpty).map(_.coerce[A])

  def cbInt[A: Coercible[Int, ?]]: Gen[A] =
    Gen.posNum[Int].map(_.coerce[A])

  def cbBigDecimal[A: Coercible[BigDecimal, ?]]: Gen[A] =
    Gen.posNum[Long].map(n => BigDecimal(n).coerce[A])

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
      n <- Gen.alphaStr.suchThat(_.nonEmpty).map(Refined.unsafeApply[String, MatchesRegex[Rgx]])
      u <- Gen.posNum[Long].map(Refined.unsafeApply[Long, Size[16]])
      x <- Gen.posNum[Int].map(x => Refined.unsafeApply[String, (Size[4] And ValidInt)](x.toString))
      c <- Gen.posNum[Int].map(_.toInt).map(Refined.unsafeApply[Int, Size[3]])
    } yield Card(n.coerce[CardName], u.coerce[CardNumber], x.coerce[CardExpiration], c.coerce[CardCCV])

}
