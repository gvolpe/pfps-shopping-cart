package shop

import cats.Parallel
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import modules._
import org.http4s.client.Client
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext
import shop.config.AppConfig

object Main extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") *>
        AppResources.make[IO](cfg).use { res =>
          Server.httpApi[IO](cfg, res).flatMap { api =>
            BlazeServerBuilder[IO]
              .bindHttp(8080, "0.0.0.0")
              .withHttpApp(api.httpApp)
              .serve
              .compile
              .drain
              .as(ExitCode.Success)
          }
        }
    }
}

object Server {

  def httpApi[F[_]: Concurrent: ContextShift: Logger: Parallel: Timer](
      cfg: AppConfig,
      res: AppResources[F]
  ): F[HttpApi[F]] =
    for {
      security <- Security.make[F](cfg, res.psql, res.redis)
      algebras <- Algebras.make[F](res.redis, res.psql, cfg.cartExpiration)
      clients <- HttpClients.make[F](res.client)
      programs <- Programs.make[F](cfg.checkoutConfig, algebras, clients)
      api <- HttpApi.make[F](algebras, programs, security)
    } yield api

}
