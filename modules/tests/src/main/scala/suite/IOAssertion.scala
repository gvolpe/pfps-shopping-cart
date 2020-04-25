package suite

import cats.effect.IO

object IOAssertion {
  def apply[A](ioa: IO[A]): Unit = ioa.void.unsafeRunSync()
}
