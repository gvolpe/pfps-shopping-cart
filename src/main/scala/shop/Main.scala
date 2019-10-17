package shop

import cats.Parallel
import cats.effect._
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import modules._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.use { client =>
      new Main[IO](client).httpApi.flatMap { api =>
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

class Main[F[_]: Concurrent: Parallel: Timer](client: Client[F]) { // HasAppConfig
  //import com.olegpy.meow.hierarchy._

  val httpApi: F[HttpApi[F]] =
    Slf4jLogger.create.flatMap { implicit logger =>
      for {
        //httpConfig <- Stream.eval(ask[F, HttpConfig])
        config <- config.load[F]
        _ <- logger.info(s"Loaded config $config")
        security <- Security.make[F](config.adminJwtConfig, config.tokenConfig)
        algebras <- Algebras.make[F]
        clients <- HttpClients.make[F](client)
        programs <- Programs.make[F](algebras, clients)
        api <- HttpApi.make[F](algebras, programs, security)
      } yield api
    }

}
