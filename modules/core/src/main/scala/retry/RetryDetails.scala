package retry

import scala.concurrent.duration.FiniteDuration

sealed trait RetryDetails {
  def retriesSoFar: Int
  def cumulativeDelay: FiniteDuration
  def givingUp: Boolean
  def upcomingDelay: Option[FiniteDuration]
}

object RetryDetails {
  final case class GivingUp(
      totalRetries: Int,
      totalDelay: FiniteDuration
  ) extends RetryDetails {
    val retriesSoFar: Int                     = totalRetries
    val cumulativeDelay: FiniteDuration       = totalDelay
    val givingUp: Boolean                     = true
    val upcomingDelay: Option[FiniteDuration] = None
  }

  final case class WillDelayAndRetry(
      nextDelay: FiniteDuration,
      retriesSoFar: Int,
      cumulativeDelay: FiniteDuration
  ) extends RetryDetails {
    val givingUp: Boolean                     = false
    val upcomingDelay: Option[FiniteDuration] = Some(nextDelay)
  }
}
