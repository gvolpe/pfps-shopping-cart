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
import shop.IOAssertion
import shop.algebras._
import shop.domain.auth._
import shop.domain.brand._
import shop.domain.cart._
import shop.domain.category._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.effects.Background
import shop.http.clients._
import shop.validation.refined._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CheckoutSpec extends AsyncFunSuite {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  val defaultBackground: Background[IO] =
    new Background[IO] {
      def schedule[A](duration: FiniteDuration, fa: IO[A]): IO[Unit] = IO.unit
    }

  def counterBackground(ref: Ref[IO, Int]): Background[IO] =
    new Background[IO] {
      def schedule[A](duration: FiniteDuration, fa: IO[A]): IO[Unit] =
        ref.update(_ + 1)
    }

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

  test("empty cart") {
    IOAssertion {
      import shop.logger.NoOp
      implicit val bg = defaultBackground
      new CheckoutProgram[IO](successfulClient, emptyCart, TestOrders(), retryPolicy)
        .checkout(testUserId, testCard)
        .attempt
        .map {
          case Left(EmptyCartError) => assert(true)
          case _                    => fail("Cart was not empty as expected")
        }
    }
  }

  test("unreachable payment client") {
    IOAssertion {
      Ref.of[IO, List[String]](List.empty).flatMap { ref =>
        implicit val bg     = defaultBackground
        implicit val logger = shop.logger.accLogger(ref)
        new CheckoutProgram[IO](unreachableClient, successfulCart, successfulOrders, retryPolicy)
          .checkout(testUserId, testCard)
          .attempt
          .flatMap {
            case Left(PaymentError(_)) =>
              ref.get.map {
                case (x :: xs) => assert(x.contains("Giving up") && xs.size == 3)
                case _         => fail("Expected 3 retries")
              }
            case _ => fail("Expected payment error")
          }
      }
    }
  }

  test("cannot create order, run in the background") {
    IOAssertion {
      Ref.of[IO, Int](0).flatMap { ref =>
        Ref.of[IO, List[String]](List.empty).flatMap { logRef =>
          implicit val bg     = counterBackground(ref)
          implicit val logger = shop.logger.accLogger(logRef)
          new CheckoutProgram[IO](successfulClient, successfulCart, failingOrders, retryPolicy)
            .checkout(testUserId, testCard)
            .attempt
            .flatMap {
              case Left(OrderError(_)) =>
                (ref.get, logRef.get).mapN {
                  case (c, (x :: y :: xs)) =>
                    assert(x.contains("Rescheduling") && y.contains("Giving up") && xs.size == 3 && c == 1)
                  case _ => fail("Expected 3 retries and reschedule")
                }
              case _ =>
                fail("Expected order error")
            }
        }
      }
    }
  }

  test("failing to delete cart does not affect checkout") {
    IOAssertion {
      import shop.logger.NoOp
      implicit val bg = defaultBackground
      new CheckoutProgram[IO](successfulClient, failingCart, successfulOrders, retryPolicy)
        .checkout(testUserId, testCard)
        .map { oid =>
          assert(oid == testOrderId)
        }
    }
  }

  test("successful checkout") {
    IOAssertion {
      import shop.logger.NoOp
      implicit val bg = defaultBackground
      new CheckoutProgram[IO](successfulClient, successfulCart, successfulOrders, retryPolicy)
        .checkout(testUserId, testCard)
        .map { oid =>
          assert(oid == testOrderId)
        }
    }
  }

}
