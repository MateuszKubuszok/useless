package useless.internal

import useless.Journal.{ ServiceState, StageStatus }
import useless.algebras.syntax._
import useless.PersistentArgument
import useless.internal.StageError.LostError

private[useless] sealed trait RecoveryStrategy {

  def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I])(
    implicit context: ServiceContext[F]
  ): F[StageError]
}

private[useless] object RecoveryStrategy {

  case object Retry extends RecoveryStrategy {

    def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I])(
      implicit context: ServiceContext[F]
    ): F[StageError] = {
      import context._
      StageError(originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Finished),
                 originalState.error.getOrElse(LostError)).pure[F]
    }
  }

  case object Revert extends RecoveryStrategy {

    def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I])(
      implicit context: ServiceContext[F]
    ): F[StageError] = {
      import context._
      StageError(originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Reverting),
                 originalState.error.getOrElse(LostError)).pure[F]
    }
  }
}
