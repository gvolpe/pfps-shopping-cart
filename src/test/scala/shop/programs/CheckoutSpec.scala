package shop.programs

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import eu.timepit.refined.auto._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.{ util => ju }
import org.scalatest.AsyncFunSuite
import retry.RetryPolicy
import retry.RetryPolicies._
import shop.PureTestSuite
import shop.algebras._
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.effects.Background
import shop.ext.refined._
import shop.http.clients._
import scala.concurrent.duration._

class CheckoutSpec extends PureTestSuite {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def randomId[A: Coercible[ju.UUID, ?]]: A = ju.UUID.randomUUID().coerce[A]

  val testPaymentId = randomId[PaymentId]
  val testOrderId   = randomId[OrderId]
  val testUserId    = randomId[UserId]

  val successfulClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(testUserId: UserId, total: USD, card: Card): IO[PaymentId] =
        IO.pure(testPaymentId)
    }

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(testUserId: UserId, total: USD, card: Card): IO[PaymentId] =
        IO.raiseError(PaymentError(""))
    }

  val failingOrders: Orders[IO] = new TestOrders {
    override def create(testUserId: UserId, testPaymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] =
      IO.raiseError(OrderError(""))
  }

  val emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(testUserId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  val failingCart: ShoppingCart[IO] = new TestCart {
    override def get(testUserId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List(CartItem(testItem, 1.coerce[Quantity])), USD(100)))
    override def delete(userId: UserId): IO[Unit] = IO.raiseError(new Exception(""))
  }
  val successfulCart: ShoppingCart[IO] = new TestCart {
    override def get(testUserId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List(CartItem(testItem, 1.coerce[Quantity])), USD(100)))
    override def delete(userId: UserId): IO[Unit] = IO.unit
  }

  val successfulOrders: Orders[IO] = new TestOrders {
    override def create(testUserId: UserId, testPaymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] =
      IO.pure(testOrderId)
  }

  val testItem = Item(
    uuid = randomId[ItemId],
    name = "Telecaster".coerce[ItemName],
    description = "Classic guitar".coerce[ItemDescription],
    price = USD(100),
    brand = Brand(randomId[BrandId], "Fender".coerce[BrandName]),
    category = Category(randomId[CategoryId], "Guitars".coerce[CategoryName])
  )

  val testCard = Card(
    name = CardName("Haskell Curry"),
    number = CardNumber(1111444433332222L),
    expiration = CardExpiration("0821"),
    ccv = CardCCV(123)
  )

  pureTest("empty cart") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    new CheckoutProgram[IO](successfulClient, emptyCart, successfulOrders, retryPolicy)
      .checkout(testUserId, testCard)
      .attempt
      .map {
        case Left(EmptyCartError) => assert(true)
        case _                    => fail("Cart was not empty as expected")
      }
  }

  pureTest("unreachable payment client") {
    Ref.of[IO, List[String]](List.empty).flatMap { logs =>
      implicit val bg     = shop.background.NoOp
      implicit val logger = shop.logger.acc(logs)
      new CheckoutProgram[IO](unreachableClient, successfulCart, successfulOrders, retryPolicy)
        .checkout(testUserId, testCard)
        .attempt
        .flatMap {
          case Left(PaymentError(_)) =>
            logs.get.map {
              case (x :: xs) => assert(x.contains("Giving up") && xs.size == MaxRetries)
              case _         => fail(s"Expected $MaxRetries retries")
            }
          case _ => fail("Expected payment error")
        }
    }
  }

  pureTest("cannot create order, run in the background") {
    Ref.of[IO, Int](0).flatMap { ref =>
      Ref.of[IO, List[String]](List.empty).flatMap { logs =>
        implicit val bg     = shop.background.counter(ref)
        implicit val logger = shop.logger.acc(logs)
        new CheckoutProgram[IO](successfulClient, successfulCart, failingOrders, retryPolicy)
          .checkout(testUserId, testCard)
          .attempt
          .flatMap {
            case Left(OrderError(_)) =>
              (ref.get, logs.get).mapN {
                case (c, (x :: y :: xs)) =>
                  assert(
                    x.contains("Rescheduling") &&
                    y.contains("Giving up") &&
                    xs.size == MaxRetries &&
                    c == 1
                  )
                case _ => fail(s"Expected $MaxRetries retries and reschedule")
              }
            case _ =>
              fail("Expected order error")
          }
      }
    }
  }

  pureTest("failing to delete cart does not affect checkout") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    new CheckoutProgram[IO](successfulClient, failingCart, successfulOrders, retryPolicy)
      .checkout(testUserId, testCard)
      .map { oid =>
        assert(oid == testOrderId)
      }
  }

  pureTest("successful checkout") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    new CheckoutProgram[IO](successfulClient, successfulCart, successfulOrders, retryPolicy)
      .checkout(testUserId, testCard)
      .map { oid =>
        assert(oid == testOrderId)
      }
  }

}
