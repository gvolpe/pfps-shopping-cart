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

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Slf4jLogger.create[IO].flatMap { implicit logger =>
      AppResources.make[IO].use { res =>
        new Main[IO].httpApi(res).flatMap { api =>
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

class Main[F[_]: Concurrent: ContextShift: Logger: Parallel: Timer]() { // HasAppConfig
  //import com.olegpy.meow.hierarchy._

  def httpApi(res: AppResources[F]): F[HttpApi[F]] =
    for {
      //httpConfig <- Stream.eval(ask[F, HttpConfig])
      security <- Security.make[F](res.cfg.adminJwtConfig, res.cfg.tokenConfig)
      algebras <- Algebras.make[F](res.psql)
      clients <- HttpClients.make[F](res.client)
      programs <- Programs.make[F](algebras, clients)
      api <- HttpApi.make[F](algebras, programs, security)
    } yield api

}
