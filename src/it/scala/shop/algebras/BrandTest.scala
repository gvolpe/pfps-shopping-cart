package shop.algebras

import cats.effect._
import cats.implicits._
import eu.timepit.refined.auto._
import io.estatico.newtype.ops._
import natchez.Trace.Implicits.noop // needed for skunk
import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext
import skunk._
import shop.PureTestSuite
import shop.config.data._
import shop.domain.brand._

class BrandTest extends PureTestSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val c = PostgreSQLConfig(
    host = "localhost",
    port = 5432,
    user = "postgres",
    database = "store",
    max = 10L
  )

  val mkSessionPool: SessionPool[IO] =
    Session
      .pooled[IO](
        host = c.host.value,
        port = c.port.value,
        user = c.user.value,
        database = c.database.value,
        max = c.max.value
      )

  spec("Brands on PostgreSQL") {
    mkSessionPool.use { pool =>
      LiveBrands.make[IO](pool).flatMap { b =>
        for {
          x <- b.findAll
          _ <- b.create("Foo".coerce[BrandName])
          y <- b.findAll
        } yield
          assert(
            x.isEmpty && y.exists(_.name.value == "Foo")
          )
      }
    }
  }

}
