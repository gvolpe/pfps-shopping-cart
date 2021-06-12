package shop.resources

import shop.config.types.HttpClientConfig

import cats.effect.kernel.{ Async, Resource }
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

// Just to demonstrate how far we can take this pattern and void hard constraints like Async
trait MkHttpClient[F[_]] {
  def newEmber(c: HttpClientConfig): Resource[F, Client[F]]
}

object MkHttpClient {
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: MkHttpClient[F] =
    new MkHttpClient[F] {
      def newEmber(c: HttpClientConfig): Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .withTimeout(c.timeout)
          .withIdleTimeInPool(c.idleTimeInPool)
          .build
    }
}
