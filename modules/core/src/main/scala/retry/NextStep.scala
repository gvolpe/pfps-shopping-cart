package retry

import scala.concurrent.duration.FiniteDuration

private[retry] sealed trait NextStep

private[retry] object NextStep {
  case object GiveUp extends NextStep

  final case class RetryAfterDelay(
      delay: FiniteDuration,
      updatedStatus: RetryStatus
  ) extends NextStep
}
