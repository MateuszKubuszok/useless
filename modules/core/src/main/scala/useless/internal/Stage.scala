package useless.internal

import useless.Journal._
import useless.PersistentArgument
import useless.algebras.syntax._

private[useless] class Stage[F[_], I: PersistentArgument, O: PersistentArgument](run: I => F[O],
                                                                                 revertOpt:        Option[O => F[I]],
                                                                                 recoveryStrategy: RecoveryStrategy) {

  private[useless] def runStage(state: ServiceState[I])(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    journalStart(state).flatMap { _ =>
      run(state.argument).flatMap(journalFinish(state, _)).recoverWith[Throwable] {
        case error: Throwable => recoveryStrategy[F, I, O](state, error).flatMap(journalFailure(_))
      }
    }
  }

  private[useless] def runRevert(state: ServiceState[O],
                                 error: Throwable)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    journalRevert(state).flatMap { _ =>
      revertOpt match {
        case Some(revert) =>
          revert(state.argument).flatMap { reverted =>
            monadError.raiseError(
              StageError(state.updateArgument(reverted).updateStageNo(_ - 1).updateStatus(StageStatus.Reverting), error)
            )
          }
        case None => monadError.raiseError(StageError.onMissingRevert(state))
      }

    }
  }

  private[useless] def restoreStage(
    rawState:         RawServiceState
  )(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    rawState.status match {
      case StageStatus.Started  => runStage(rawState.as[I])
      case StageStatus.Finished => rawState.as[O].pure[F]
      case StageStatus.Failed =>
        recoveryStrategy[F, I, O](rawState.as[I], StageError.Restored).flatMap(journalFailure(_))
      case StageStatus.Reverting => runRevert(rawState.as[O], StageError.Restored)
      case StageStatus.IllegalState =>
        (new IllegalStateException("Service is in illegal state"): Throwable).raiseError[F, ServiceState[O]]
    }
  }

  // service state journaling

  private def journalStart(state: ServiceState[I])(implicit context: ServiceContext[F]): F[Unit] =
    context.journal.persistState[I](state.updateStatus(StageStatus.Started))

  private def journalFinish(state:  ServiceState[I],
                            output: O)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context.{ serviceName => _, _ }
    val newState = state.updateArgument(output).updateStatus(StageStatus.Finished)
    for {
      _ <- journal.persistState[O](newState)
    } yield newState
  }

  private def journalFailure(stageError: StageError)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    journal.persistRawState(stageError.recoveredState).flatMap { _ =>
      monadError.raiseError[ServiceState[O]](stageError)
    }
  }

  private def journalRevert(state: ServiceState[O])(implicit context: ServiceContext[F]): F[Unit] =
    context.journal.persistState[O](state.updateStatus(StageStatus.Reverting))
}

private[useless] object Stage {

  def apply[F[_], I: PersistentArgument, O: PersistentArgument](run: I => F[O],
                                                                revert:           O => F[I],
                                                                recoveryStrategy: RecoveryStrategy): Stage[F, I, O] =
    new Stage(run, Some(revert), recoveryStrategy)

  def apply[F[_], I: PersistentArgument, O: PersistentArgument](run: I => F[O],
                                                                recoveryStrategy: RecoveryStrategy): Stage[F, I, O] =
    new Stage(run, None, recoveryStrategy)
}
