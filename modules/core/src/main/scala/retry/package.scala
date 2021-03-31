import scala.annotation.nowarn

import cats.effect.Temporal
import cats.syntax.all._
import cats.{ Monad, MonadThrow }

package object retry {
  private def retryingOnSomeErrorsImpl[M[_], A](
      policy: RetryPolicy[M],
      isWorthRetrying: Throwable => Boolean,
      onError: (Throwable, RetryDetails) => M[Unit],
      status: RetryStatus,
      attempt: Either[Throwable, A]
  )(
      implicit
      ME: MonadThrow[M],
      S: Temporal[M]
  ): M[Either[RetryStatus, A]] = attempt match {
    case Left(error) if isWorthRetrying(error) =>
      for {
        nextStep <- applyPolicy(policy, status)
        _ <- onError(error, buildRetryDetails(status, nextStep))
        result <- nextStep match {
                   case NextStep.RetryAfterDelay(delay, updatedStatus) =>
                     S.sleep(delay) *>
                         ME.pure(Left(updatedStatus)) // continue recursion
                   case NextStep.GiveUp =>
                     ME.raiseError[A](error).map(Right(_)) // stop the recursion
                 }
      } yield result
    case Left(error) =>
      ME.raiseError[A](error).map(Right(_)) // stop the recursion
    case Right(success) =>
      ME.pure(Right(success)) // stop the recursion
  }

  def retryingOnAllErrors[A] = new RetryingOnAllErrorsPartiallyApplied[A]

  @nowarn
  private[retry] class RetryingOnAllErrorsPartiallyApplied[A] {
    def apply[M[_]](
        policy: RetryPolicy[M],
        onError: (Throwable, RetryDetails) => M[Unit]
    )(
        action: => M[A]
    )(
        implicit
        ME: MonadThrow[M],
        S: Temporal[M]
    ): M[A] = ME.tailRecM(RetryStatus.NoRetriesYet) { status =>
      ME.attempt(action).flatMap { attempt =>
        retryingOnSomeErrorsImpl(
          policy,
          _ => true,
          onError,
          status,
          attempt
        )
      }
    }
  }

  def noop[M[_]: Monad, A]: (A, RetryDetails) => M[Unit] =
    (_, _) => Monad[M].pure(())

  private[retry] def applyPolicy[M[_]: Monad](
      policy: RetryPolicy[M],
      retryStatus: RetryStatus
  ): M[NextStep] =
    policy.decideNextRetry(retryStatus).map {
      case PolicyDecision.DelayAndRetry(delay) =>
        NextStep.RetryAfterDelay(delay, retryStatus.addRetry(delay))
      case PolicyDecision.GiveUp =>
        NextStep.GiveUp
    }

  private[retry] def buildRetryDetails(
      currentStatus: RetryStatus,
      nextStep: NextStep
  ): RetryDetails =
    nextStep match {
      case NextStep.RetryAfterDelay(delay, _) =>
        RetryDetails.WillDelayAndRetry(
          delay,
          currentStatus.retriesSoFar,
          currentStatus.cumulativeDelay
        )
      case NextStep.GiveUp =>
        RetryDetails.GivingUp(
          currentStatus.retriesSoFar,
          currentStatus.cumulativeDelay
        )
    }

}
