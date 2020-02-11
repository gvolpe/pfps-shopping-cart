package suite

import cats.Eq
import org.scalactic._
import org.scalactic.TripleEqualsSupport.AToBEquivalenceConstraint
import org.scalactic.TripleEqualsSupport.BToAEquivalenceConstraint

// Credits to the original implementation: https://github.com/bvenners/equality-integration-demo
final class CatsEquivalence[A](ev: Eq[A]) extends Equivalence[A] {
  def areEquivalent(a: A, b: A): Boolean = ev.eqv(a, b)
}

trait LowPriorityCatsConstraints extends TripleEquals {
  implicit def lowPriorityCatsConstraint[A, B](implicit ev1: Eq[B], ev2: A => B): CanEqual[A, B] =
    new AToBEquivalenceConstraint[A, B](new CatsEquivalence(ev1), ev2)
}

trait CatsEquality extends LowPriorityCatsConstraints {
  override def convertToEqualizer[T](left: T): Equalizer[T]                          = super.convertToEqualizer[T](left)
  implicit override def convertToCheckingEqualizer[T](left: T): CheckingEqualizer[T] = new CheckingEqualizer(left)
  override def unconstrainedEquality[A, B](implicit equalityOfA: Equality[A]): CanEqual[A, B] =
    super.unconstrainedEquality[A, B]
  implicit def bToAConstraint[A, B](implicit ev1: Eq[A], ev2: B => A): CanEqual[A, B] =
    new BToAEquivalenceConstraint[A, B](new CatsEquivalence(ev1), ev2)
}
