package shop

import cats._
import cats.effect._
import cats.implicits._
import com.olegpy.meow.hierarchy._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder
import shop.config.data._
import shop.effects._
import shop.modules._

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]

  def loadResources[F[_]: ConcurrentEffect: ContextShift: FlatMap: HasAppConfig: Logger](
      fa: AppConfig => AppResources[F] => F[ExitCode]
  ): F[ExitCode] =
    F.ask.flatMap { cfg =>
      F.info(s"Loaded config $cfg") >>
        AppResources.make[F].use(res => fa(cfg)(res))
    }

  override def run(args: List[String]): IO[ExitCode] =
    loadResources[IO] { cfg => res =>
      for {
        security <- Security.make[IO](cfg, res.psql, res.redis)
        algebras <- Algebras.make[IO](res.redis, res.psql, cfg.cartExpiration)
        clients <- HttpClients.make[IO](cfg.paymentConfig, res.client)
        programs <- Programs.make[IO](cfg.checkoutConfig, algebras, clients)
        api <- HttpApi.make[IO](algebras, programs, security)
        _ <- BlazeServerBuilder[IO]
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
