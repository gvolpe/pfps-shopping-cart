package retry

import scala.concurrent.duration.{ Duration, FiniteDuration }

final case class RetryStatus(
    retriesSoFar: Int,
    cumulativeDelay: FiniteDuration,
    previousDelay: Option[FiniteDuration]
) {
  def addRetry(delay: FiniteDuration): RetryStatus = RetryStatus(
    retriesSoFar = this.retriesSoFar + 1,
    cumulativeDelay = this.cumulativeDelay + delay,
    previousDelay = Some(delay)
  )
}

object RetryStatus {
  val NoRetriesYet = RetryStatus(0, Duration.Zero, None)
}
