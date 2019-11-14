package shop.programs

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import io.estatico.newtype.ops._
import java.util.UUID
import org.scalatest.AsyncFunSuite
import retry.RetryPolicy
import retry.RetryPolicies._
import shop.algebras._
import shop.arbitraries._
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
import shop.suite.PureTestSuite
import scala.concurrent.duration._

final class CheckoutSpec extends PureTestSuite {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  val testPaymentId = randomId[PaymentId]
  val testOrderId   = randomId[OrderId]
  val testUserId    = randomId[UserId]

  val successfulClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(userId: UserId, total: USD, card: Card): IO[PaymentId] =
        IO.pure(testPaymentId)
    }

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(userId: UserId, total: USD, card: Card): IO[PaymentId] =
        IO.raiseError(PaymentError(""))
    }

  def recoveringClient(ref: Ref[IO, Int]): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(userId: UserId, total: USD, card: Card): IO[PaymentId] =
        ref.get.flatMap {
          case n if n == 1 => IO.pure(testPaymentId)
          case _           => ref.update(_ + 1) *> IO.raiseError(PaymentError(""))
        }
    }

  val failingOrders: Orders[IO] = new TestOrders {
    override def create(userId: UserId, testPaymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] =
      IO.raiseError(OrderError(""))
  }

  val emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] = IO.raiseError(new Exception(""))
  }

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] = IO.unit
  }

  val successfulOrders: Orders[IO] = new TestOrders {
    override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] =
      IO.pure(testOrderId)
  }

  forAll { (card: Card, id: UUID) =>
    spec(s"empty cart - $id") {
      implicit val bg = shop.background.NoOp
      import shop.logger.NoOp
      new CheckoutProgram[IO](successfulClient, emptyCart, successfulOrders, retryPolicy)
        .checkout(testUserId, card)
        .attempt
        .map {
          case Left(EmptyCartError) => assert(true)
          case _                    => fail("Cart was not empty as expected")
        }
    }
  }

  forAll { (ct: CartTotal, card: Card, id: UUID) =>
    spec(s"unreachable payment client - $id") {
      Ref.of[IO, List[String]](List.empty).flatMap { logs =>
        implicit val bg     = shop.background.NoOp
        implicit val logger = shop.logger.acc(logs)
        new CheckoutProgram[IO](unreachableClient, successfulCart(ct), successfulOrders, retryPolicy)
          .checkout(testUserId, card)
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
  }

  forAll { (ct: CartTotal, card: Card, id: UUID) =>
    spec(s"failing payment client succeeds after one retry - $id") {
      Ref.of[IO, List[String]](List.empty).flatMap { logs =>
        Ref.of[IO, Int](0).flatMap { ref =>
          implicit val bg     = shop.background.NoOp
          implicit val logger = shop.logger.acc(logs)
          new CheckoutProgram[IO](recoveringClient(ref), successfulCart(ct), successfulOrders, retryPolicy)
            .checkout(testUserId, card)
            .attempt
            .flatMap {
              case Right(oid) =>
                logs.get.map { xs =>
                  assert(oid == testOrderId && xs.size == 1)
                }
              case Left(_) => fail("Expected Payment Id")
            }
        }
      }
    }
  }

  forAll { (ct: CartTotal, card: Card, id: UUID) =>
    spec(s"cannot create order, run in the background - $id") {
      Ref.of[IO, Int](0).flatMap { ref =>
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          implicit val bg     = shop.background.counter(ref)
          implicit val logger = shop.logger.acc(logs)
          new CheckoutProgram[IO](successfulClient, successfulCart(ct), failingOrders, retryPolicy)
            .checkout(testUserId, card)
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
  }

  forAll { (ct: CartTotal, card: Card, id: UUID) =>
    spec(s"failing to delete cart does not affect checkout - $id") {
      implicit val bg = shop.background.NoOp
      import shop.logger.NoOp
      new CheckoutProgram[IO](successfulClient, failingCart(ct), successfulOrders, retryPolicy)
        .checkout(testUserId, card)
        .map { oid =>
          assert(oid == testOrderId)
        }
    }
  }

  forAll { (ct: CartTotal, card: Card, id: UUID) =>
    spec(s"successful checkout - $id") {
      implicit val bg = shop.background.NoOp
      import shop.logger.NoOp
      new CheckoutProgram[IO](successfulClient, successfulCart(ct), successfulOrders, retryPolicy)
        .checkout(testUserId, card)
        .map { oid =>
          assert(oid == testOrderId)
        }
    }
  }

}
