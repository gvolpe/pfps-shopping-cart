package suite

import cats.effect._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.compatible.Assertion
import org.scalactic.source.Position

trait ResourceSuite[A] extends PureTestSuite with BeforeAndAfterAll {

  def resource: Resource[IO, A]

  private[this] var res: A            = _
  private[this] var cleanUp: IO[Unit] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (r, h) = resource.allocated.unsafeRunSync()
    res = r
    cleanUp = h
  }

  override def afterAll(): Unit = {
    cleanUp.unsafeRunSync()
    super.afterAll()
  }

  def specR(testName: String)(f: A => IO[Assertion])(implicit pos: Position): Unit =
    spec(testName)(f(res))

}
