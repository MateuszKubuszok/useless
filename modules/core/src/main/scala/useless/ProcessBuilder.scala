package useless

import useless.internal.{ RecoveryStrategy, Stage }
import useless.ProcessBuilder.CustomStrategy
import useless.algebras.{ MonadThrowable, Timer }

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

  def customHandler[O2](
    run: O => F[O2]
  )(
    revert: O2 => F[O]
  )(
    customStrategy: CustomStrategy
  )(implicit po:    PersistentArgument[O], po2: PersistentArgument[O2]): ReversibleProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, revert, RecoveryStrategy.Custom(customStrategy)))

  def customHandlerNonRevertible[O2](
    run: O => F[O2]
  )(
    customStrategy: CustomStrategy
  )(implicit po:    PersistentArgument[O], po2: PersistentArgument[O2]): ReversibleProcessBuilder[F, I, O2] =
    ProcessBuilder.Proceed[F, I, O, O2](this, Stage(run, RecoveryStrategy.Custom(customStrategy)))
}

object ProcessBuilder {

  def create[F[_], A]: ReversibleProcessBuilder[F, A, A] = Init()

  private[useless] final case class Init[F[_], A]() extends ReversibleProcessBuilder[F, A, A]
  private[useless] final case class Proceed[F[_], I, M, O](current: ProcessBuilder[F, I, M], next: Stage[F, M, O])
      extends ReversibleProcessBuilder[F, I, O]

  sealed trait OnError
  object OnError {
    case object Retry extends OnError
    case object Revert extends OnError
  }

  trait CustomStrategy {

    def apply[F[_]: MonadThrowable: Timer, I: PersistentArgument](argument: I): F[(I, OnError)]
  }

  implicit def withBoundedRetry[F[_], I, O](rpc: ReversibleProcessBuilder[F, I, O]): extra.BoundedRetryOps[F, I, O] =
    new extra.BoundedRetryOps[F, I, O](rpc)
}
