package shop

import shop.modules._

import cats.effect._
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources
          .make[IO](cfg)
          .evalMap { res =>
            Security.make[IO](cfg, res.psql, res.redis).map { security =>
              val clients  = HttpClients.make[IO](cfg.paymentConfig, res.client)
              val services = Services.make[IO](res.redis, res.psql, cfg.cartExpiration)
              val programs = Programs.make[IO](cfg.checkoutConfig, services, clients)
              val api      = HttpApi.make[IO](services, programs, security)
              cfg.httpServerConfig -> api
            }
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
            Logger[IO].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${server.address}") >>
              IO.never.as(ExitCode.Success)
          }
    }

}
