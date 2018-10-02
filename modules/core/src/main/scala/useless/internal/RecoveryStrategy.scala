package useless.internal

import useless.Journal.{ ServiceState, StageStatus }
import useless.PersistentArgument

private[useless] sealed trait RecoveryStrategy {

  def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I], error: Throwable)(
    implicit context: ServiceContext[F]
  ): F[StageError]
}

private[useless] object RecoveryStrategy {

  val retryUntilSucceed: RecoveryStrategy = new RecoveryStrategy {

    def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I], error: Throwable)(
      implicit context: ServiceContext[F]
    ): F[StageError] = {
      import context._
      monadError.pure(StageError(originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Finished), error))
    }
  }
}
