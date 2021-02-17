package shop

import java.util.UUID

import cats.Eq
import io.estatico.newtype.Coercible

package object domain {

  // does not work as implicit for some reason...
  private def coercibleEq[A: Eq, B: Coercible[A, *]]: Eq[B] =
    new Eq[B] {
      def eqv(x: B, y: B): Boolean =
        Eq[A].eqv(x.asInstanceOf[A], y.asInstanceOf[A])
    }

  implicit def coercibleStringEq[B: Coercible[String, *]]: Eq[B] =
    coercibleEq[String, B]

  implicit def coercibleUuidEq[B: Coercible[UUID, *]]: Eq[B] =
    coercibleEq[UUID, B]

  implicit def coercibleIntEq[B: Coercible[Int, *]]: Eq[B] =
    coercibleEq[Int, B]

}
