package shop.programs

import scala.util.control.NoStackTrace

import shop.algebras._
import shop.arbitraries._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.domain.payment._
import shop.http.clients._

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import org.scalacheck.Prop._
import retry.RetryPolicies._
import retry.RetryPolicy
import squants.market._
import suite._

final class CheckoutSuite extends PureTestSuite {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.pure(paymentId)
    }

  val unreachableClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        IO.raiseError(PaymentError(""))
    }

  def recoveringClient(attemptsSoFar: Ref[IO, Int], paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(payment: Payment): IO[PaymentId] =
        attemptsSoFar.get.flatMap {
          case n if n === 1 => IO.pure(paymentId)
          case _            => attemptsSoFar.update(_ + 1) *> IO.raiseError(PaymentError(""))
        }
    }

  val failingOrders: Orders[IO] = new TestOrders {
    override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] =
      IO.raiseError(OrderError(""))
  }

  val emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] = IO.raiseError(new NoStackTrace {})
  }

  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] = IO.unit
  }

  def successfulOrders(orderId: OrderId): Orders[IO] = new TestOrders {
    override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] =
      IO.pure(orderId)
  }

  test("empty cart") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](successfulClient(pid), emptyCart, successfulOrders(oid), retryPolicy)
          .checkout(uid, card)
          .attempt
          .map {
            case Left(EmptyCartError) => assert(true)
            case _                    => fail("Cart was not empty as expected")
          }
      }
    }
  }

  test("unreachable payment client") {
    forAll { (uid: UserId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          implicit val bg     = shop.background.NoOp
          implicit val logger = shop.logger.acc(logs)
          new CheckoutProgram[IO](unreachableClient, successfulCart(ct), successfulOrders(oid), retryPolicy)
            .checkout(uid, card)
            .attempt
            .flatMap {
              case Left(PaymentError(_)) =>
                logs.get.map {
                  case (x :: xs) => assert(x.contains("Giving up") && xs.size === MaxRetries)
                  case _         => fail(s"Expected $MaxRetries retries")
                }
              case _ => fail("Expected payment error")
            }
        }
      }
    }
  }

  test("failing payment client succeeds after one retry") {
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          Ref.of[IO, Int](0).flatMap { ref =>
            implicit val bg     = shop.background.NoOp
            implicit val logger = shop.logger.acc(logs)
            new CheckoutProgram[IO](recoveringClient(ref, pid), successfulCart(ct), successfulOrders(oid), retryPolicy)
              .checkout(uid, card)
              .attempt
              .flatMap {
                case Right(id) =>
                  logs.get.map { xs =>
                    assert(id === oid && xs.size === 1)
                  }
                case Left(_) => fail("Expected Payment Id")
              }
          }
        }
      }
    }
  }

  test("cannot create order, run in the background") {
    forAll { (uid: UserId, pid: PaymentId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, Int](0).flatMap { ref =>
          Ref.of[IO, List[String]](List.empty).flatMap { logs =>
            implicit val bg     = shop.background.counter(ref)
            implicit val logger = shop.logger.acc(logs)
            new CheckoutProgram[IO](successfulClient(pid), successfulCart(ct), failingOrders, retryPolicy)
              .checkout(uid, card)
              .attempt
              .flatMap {
                case Left(OrderError(_)) =>
                  (ref.get, logs.get).mapN {
                    case (c, (x :: y :: xs)) =>
                      assert(
                        x.contains("Rescheduling") &&
                          y.contains("Giving up") &&
                          xs.size === MaxRetries &&
                          c === 1
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
  }

  test("failing to delete cart does not affect checkout") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](successfulClient(pid), failingCart(ct), successfulOrders(oid), retryPolicy)
          .checkout(uid, card)
          .map { id =>
            assert(id === oid)
          }
      }
    }
  }

  test(s"successful checkout") {
    implicit val bg = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](successfulClient(pid), successfulCart(ct), successfulOrders(oid), retryPolicy)
          .checkout(uid, card)
          .map { id =>
            assert(id === oid)
          }
      }
    }
  }

}

protected class TestOrders() extends Orders[IO] {
  def get(userId: UserId, orderId: OrderId): IO[Option[Order]]                                       = ???
  def findBy(userId: UserId): IO[List[Order]]                                                        = ???
  def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] = ???
}

protected class TestCart() extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
  def get(userId: UserId): IO[CartTotal]                                = ???
  def delete(userId: UserId): IO[Unit]                                  = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = ???
  def update(userId: UserId, cart: Cart): IO[Unit]                      = ???
}
