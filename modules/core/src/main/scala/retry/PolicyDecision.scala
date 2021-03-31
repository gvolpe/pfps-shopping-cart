package retry

import scala.concurrent.duration.FiniteDuration

sealed trait PolicyDecision

object PolicyDecision {
  case object GiveUp extends PolicyDecision

  final case class DelayAndRetry(
      delay: FiniteDuration
  ) extends PolicyDecision
}
