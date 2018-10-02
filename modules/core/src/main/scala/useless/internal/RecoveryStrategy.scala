package useless.internal

import useless.Journal.{ ServiceState, StageStatus }
import useless.algebras.syntax._
import useless.PersistentArgument
import useless.ProcessBuilder.{ CustomStrategy, OnError }

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
      originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Finished).toStageError.pure[F]
    }
  }

  case object Revert extends RecoveryStrategy {

    def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I])(
      implicit context: ServiceContext[F]
    ): F[StageError] = {
      import context._
      originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Reverting).toStageError.pure[F]
    }
  }

  final case class Custom(byArgument: CustomStrategy) extends RecoveryStrategy {

    def apply[F[_], I: PersistentArgument, O: PersistentArgument](originalState: ServiceState[I])(
      implicit context: ServiceContext[F]
    ): F[StageError] =
      byArgument(originalState.argument) match {
        case OnError.Retry  => Retry[F, I, O](originalState)
        case OnError.Revert => Revert[F, I, O](originalState)
      }
  }
}
