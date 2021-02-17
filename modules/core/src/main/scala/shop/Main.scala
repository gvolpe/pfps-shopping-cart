package shop

import scala.concurrent.ExecutionContext

import shop.modules._

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { res =>
          for {
            security <- Security.make[IO](cfg, res.psql, res.redis)
            algebras <- Algebras.make[IO](res.redis, res.psql, cfg.cartExpiration)
            clients <- HttpClients.make[IO](cfg.paymentConfig, res.client)
            programs <- Programs.make[IO](cfg.checkoutConfig, algebras, clients)
            api <- HttpApi.make[IO](algebras, programs, security)
            _ <- BlazeServerBuilder[IO](ExecutionContext.global)
                  .bindHttp(
                    cfg.httpServerConfig.port.value,
                    cfg.httpServerConfig.host.value
                  )
                  .withHttpApp(api.httpApp)
                  .serve
                  .compile
                  .drain
          } yield ExitCode.Success
        }
    }

}
