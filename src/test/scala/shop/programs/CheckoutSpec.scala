package shop.programs

import cats.effect._
import cats.effect.concurrent.Ref
import cats.effect.laws.util.TestContext
import cats.implicits._
import eu.timepit.refined.auto._
import io.estatico.newtype.ops._
import java.{ util => ju }
import org.scalatest.AsyncFunSuite
import shop.IOAssertion
import shop.algebras._
import shop.domain.auth._
import shop.domain.cart._
import shop.domain.checkout._
import shop.domain.item._
import shop.domain.order._
import shop.effects.Background
import shop.http.clients._
import shop.logger.NoOp
import shop.validation.refined._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CheckoutSpec extends AsyncFunSuite {

  val ec: ExecutionContext = TestContext()

  implicit val timer: Timer[IO] = IO.timer(ec)

  def mkBackground: IO[Background[IO]] =
    Ref.of[IO, Int](0).map { ref =>
      new Background[IO] {
        def schedule[A](duration: FiniteDuration, fa: IO[A]): IO[Unit] =
          ref.update(_ + 1)
      }
    }

  val paymentId = ju.UUID.fromString("7a465b27-0db6-4cb7-8c98-78f275b0235e").coerce[PaymentId]
  val userId    = ju.UUID.fromString("509b77fd-3a0e-4aa3-9d84-496a3211d2b1").coerce[UserId]

  // TODO: Also create a bad client that returns 409 and 500
  val testClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(userId: UserId, total: USD, card: Card): IO[PaymentId] =
        IO.pure(paymentId)
    }

  val emptyCart: ShoppingCart[IO] =
    new ShoppingCart[IO] {
      def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
      def get(userId: UserId): IO[CartTotal] =
        IO.pure(CartTotal(List.empty, USD(0)))
      def delete(userId: UserId): IO[Unit]                     = ???
      def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = ???
      def update(userId: UserId, cart: Cart): IO[Unit]         = ???
    }

  val testCart: ShoppingCart[IO] =
    new ShoppingCart[IO] {
      def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
      def get(userId: UserId): IO[CartTotal]                                = ???
      def delete(userId: UserId): IO[Unit]                                  = ???
      def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = ???
      def update(userId: UserId, cart: Cart): IO[Unit]                      = ???
    }

  // TODO: consider errors for rety logic
  val testOrders: Orders[IO] =
    new Orders[IO] {
      def get(userId: UserId, orderId: OrderId): IO[Option[Order]]                                     = ???
      def findBy(userId: UserId): IO[List[Order]]                                                      = ???
      def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: USD): IO[OrderId] = ???
    }

  val mkProgram = mkBackground.map { implicit bg =>
    new CheckoutProgram[IO](testClient, testCart, testOrders)
  }

  val testCard = Card(
    name = CardName("Haskell Curry"),
    number = CardNumber(1111444433332222L),
    expiration = CardExpiration(4208),
    ccv = CardCCV(123)
  )

  test("empty cart on checkout") {
    IOAssertion {
      mkBackground.flatMap { implicit bg =>
        val program = new CheckoutProgram[IO](testClient, emptyCart, testOrders)
        program.checkout(userId, testCard).attempt.map {
          case Left(EmptyCartError) => assert(true)
          case _                    => fail("Cart was not empty as expected")
        }
      }
    }
  }

}
