package shop.http.errors

import cats.{ ApplicativeError, MonadError }
import cats.data.{ Kleisli, OptionT }
import org.http4s._
import org.http4s.dsl.Http4sDsl
import shop.domain.errors._

trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

abstract class RoutesHttpErrorHandler[F[_], E <: Throwable] extends HttpErrorHandler[F, E] with Http4sDsl[F] {
  def A: ApplicativeError[F, E]
  def handler: E => F[Response[F]]
  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT {
        A.handleErrorWith(routes.run(req).value)(e => A.map(handler(e))(Option(_)))
      }
    }
}

object HttpErrorHandler {
  @inline final def apply[F[_], E <: Throwable](implicit ev: HttpErrorHandler[F, E]) = ev
}

// -- instances

object CheckoutHttpErrorHandler {
  def apply[F[_]: MonadError[*[_], CheckoutError]]: HttpErrorHandler[F, CheckoutError] =
    new RoutesHttpErrorHandler[F, CheckoutError] {
      val A = implicitly

      val handler: CheckoutError => F[Response[F]] = {
        case CartNotFound(userId) =>
          NotFound(s"Cart not found for user: ${userId.value}")
        case EmptyCartError =>
          BadRequest("Shopping cart is empty!")
        case PaymentError(cause) =>
          BadRequest(cause)
        case OrderError(cause) =>
          BadRequest(cause)
      }
    }
}
