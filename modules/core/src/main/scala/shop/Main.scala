package shop

import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder
import shop.modules._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends CatsApp {

  implicit val logger = Slf4jLogger.getLogger[Task]

  override def run(args: List[String]): UIO[Int] =
    config
      .load[Task]
      .flatMap { cfg =>
        Logger[Task].info(s"Loaded config $cfg") *>
          AppResources.make[Task](cfg).use { res =>
            for {
              security <- Security.make[Task](cfg, res.psql, res.redis)
              algebras <- Algebras.make[Task](res.redis, res.psql, cfg.cartExpiration)
              clients <- HttpClients.make[Task](cfg.paymentConfig, res.client)
              programs <- Programs.make[Task](cfg.checkoutConfig, algebras, clients)
              api <- HttpApi.make[Task](algebras, programs, security)
              _ <- BlazeServerBuilder[Task]
                    .bindHttp(
                      cfg.httpServerConfig.port.value,
                      cfg.httpServerConfig.host.value
                    )
                    .withHttpApp(api.httpApp)
                    .serve
                    .compile
                    .drain
            } yield 0
          }
      }
      .orDie

}
