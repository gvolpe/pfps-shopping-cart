package suite

import cats.effect._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.compatible.Assertion
import org.scalactic.source.Position
import cats.effect.concurrent.Deferred

trait ResourceSuite[A] extends PureTestSuite with BeforeAndAfterAll {

  def resources: Resource[IO, A]

  private[this] var res: A            = _
  private[this] var cleanUp: IO[Unit] = _

  private[this] val latch = Deferred[IO, Unit].unsafeRunSync()

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (r, h) = resources.allocated.unsafeRunSync()
    res = r
    cleanUp = h
    latch.complete(()).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    cleanUp.unsafeRunSync()
    super.afterAll()
  }

  def withResources(f: (=> A) => Unit): Unit = f {
    //to ensure that the resource has been allocated even before any spec(...) bodies
    latch.get.unsafeRunSync
    res
  }
}
