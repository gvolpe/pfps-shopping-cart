package shop

import cats.Parallel
import cats.effect._
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import modules._
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Main[IO].httpApi.flatMap { api =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(api.httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }

}

class Main[F[_]: Concurrent: Parallel: Timer] { // HasAppConfig
  //import com.olegpy.meow.hierarchy._

  val httpApi: F[HttpApi[F]] =
    Slf4jLogger.create.flatMap { implicit logger =>
      for {
        //httpConfig <- Stream.eval(ask[F, HttpConfig])
        config <- config.load[F]
        _ <- logger.info(s"Loaded config $config")
        services <- Services.make[F](config.adminJwtConfig, config.tokenConfig)
        api <- HttpApi.make[F](services)
      } yield api
    }

}
