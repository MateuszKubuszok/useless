package useless

import useless.internal.{ RecoveryStrategy, Stage }

sealed trait ProcessBuilder[F[_], I, O] {

  def retryUntilSucceedNonRevertible[O2](
    run:         O => F[O2]
  )(implicit po: PersistentArgument[O], po2: PersistentArgument[O2]): ProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, RecoveryStrategy.Retry))
}

sealed trait ReversibleProcessBuilder[F[_], I, O] extends ProcessBuilder[F, I, O] {

  def retryUntilSucceed[O2](
    run: O => F[O2]
  )(
    revert:      O2 => F[O]
  )(implicit po: PersistentArgument[O], po2: PersistentArgument[O2]): ReversibleProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, revert, RecoveryStrategy.Retry))

  def revertOnFirstFailure[O2](
    run: O => F[O2]
  )(
    revert:      O2 => F[O]
  )(implicit po: PersistentArgument[O], po2: PersistentArgument[O2]): ReversibleProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, revert, RecoveryStrategy.Revert))

  def revertOnFirstFailureNonRevertible[O2](
    run:         O => F[O2]
  )(implicit po: PersistentArgument[O], po2: PersistentArgument[O2]): ProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, RecoveryStrategy.Revert))
}

object ProcessBuilder {

  def create[F[_], A]: ReversibleProcessBuilder[F, A, A] = Init()

  private[useless] final case class Init[F[_], A]() extends ReversibleProcessBuilder[F, A, A]
  private[useless] final case class Proceed[F[_], I, M, O](current: ProcessBuilder[F, I, M], next: Stage[F, M, O])
      extends ReversibleProcessBuilder[F, I, O]
}
