package shop

import cats.Parallel
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import config.AppConfig
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import modules._
import natchez.Trace.Implicits.noop // needed for skunk
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext
import skunk._

object Main extends IOApp {

  // TODO: User PSQL config file
  //def psql(cfg: AppConfig): Resource[IO, Session[IO]] =
  def psql(): Resource[IO, Session[IO]] =
    Session
      .single(
        host = "localhost",
        port = 5432,
        user = "postgres",
        database = "store"
      )

  def resources(implicit L: Logger[IO]): Resource[IO, (AppConfig, Client[IO], Session[IO])] =
    for {
      cfg <- Resource.liftF(config.load[IO])
      _ <- Resource.liftF(L.info(s"Loaded config $cfg"))
      client <- BlazeClientBuilder[IO](ExecutionContext.global).resource
      session <- psql()
    } yield (cfg, client, session)

  override def run(args: List[String]): IO[ExitCode] =
    Slf4jLogger.create[IO].flatMap { implicit logger =>
      resources.use {
        case (cfg, client, session) =>
          val app = new Main[IO]
          app.httpApi(cfg, client, session).flatMap { api =>
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

  def httpApi(
      cfg: AppConfig,
      client: Client[F],
      session: Session[F]
  ): F[HttpApi[F]] =
    for {
      //httpConfig <- Stream.eval(ask[F, HttpConfig])
      security <- Security.make[F](cfg.adminJwtConfig, cfg.tokenConfig)
      algebras <- Algebras.make[F](session)
      clients <- HttpClients.make[F](client)
      programs <- Programs.make[F](algebras, clients)
      api <- HttpApi.make[F](algebras, programs, security)
    } yield api

}
