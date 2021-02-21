package shop

import shop.modules._

import cats.effect._
import cats.syntax.all._
import eu.timepit.refined.auto._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.ember.server.EmberServerBuilder

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources
          .make[IO](cfg)
          .evalMap { res =>
            for {
              security <- Security.make[IO](cfg, res.psql, res.redis)
              algebras <- Algebras.make[IO](res.redis, res.psql, cfg.cartExpiration)
              clients <- HttpClients.make[IO](cfg.paymentConfig, res.client)
              programs <- Programs.make[IO](cfg.checkoutConfig, algebras, clients)
              api <- HttpApi.make[IO](algebras, programs, security)
            } yield cfg.httpServerConfig -> api
          }
          .flatMap {
            case (cfg, api) =>
              EmberServerBuilder
                .default[IO]
                .withHost(cfg.host)
                .withPort(cfg.port)
                .withHttpApp(api.httpApp)
                .build
          }
          .use { server =>
            Logger[IO].info(s"HTTP Server started at ${server.address}") >>
              IO.never.as(ExitCode.Success)
          }
    }

}
