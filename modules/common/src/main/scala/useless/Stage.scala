package useless

import useless.Journalist._
import useless.internal._
import useless.syntax._

sealed abstract class Stage[F[_], I: PersistentArgument, O: PersistentArgument] {

  // stage-type specific

  private[useless] val f: I => F[O]
  private[useless] def recoverStrategy(originalState: ServiceState[I], error: Throwable)(
    implicit context:                                 ServiceContext[F]
  ): F[StageError]

  // common for stages

  private[useless] def runStage(state: ServiceState[I])(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    markStarted(state).flatMap { _ =>
      f(state.argument).flatMap(markFinished(state, _)).recoverWith {
        case error: Throwable => recoverStrategy(state, error).flatMap(markFailed(_))
      }
    }
  }

  private[useless] def restoreStage(
    rawState:         RawServiceState
  )(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    rawState.status match {
      case StageStatus.Started  => runStage(rawState.as[I])
      case StageStatus.Finished => monadError.pure(rawState.as[O])
      case StageStatus.Failed   => recoverStrategy(rawState.as[I], StageError.Restored).flatMap(markFailed(_))
    }
  }

  // service state journaling

  private def markStarted(state: ServiceState[I])(implicit context: ServiceContext[F]): F[Unit] = {
    val newState = state.updateStatus(StageStatus.Started)
    context.journalist.persistStatus[I](newState)
  }

  private def markFinished(state:  ServiceState[I],
                           output: O)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context.{ serviceName => _, _ }
    val newState = state.updateArgument(output).updateStatus(StageStatus.Finished)
    for {
      _ <- journalist.persistStatus[O](newState)
    } yield newState
  }

  private def markFailed(stageError: StageError)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    journalist.persistRawStatus(stageError.recoveredState).flatMap { _ =>
      monadError.raiseError[ServiceState[O]](stageError)
    }
  }
}

object Stage {

  // on failure rewinds state to finished previous stage
  private[useless] final case class RetryUntilSucceed[F[_], I: PersistentArgument, O: PersistentArgument](f: I => F[O])
      extends Stage[F, I, O] {

    def recoverStrategy(originalState: ServiceState[I],
                        error:         Throwable)(implicit context: ServiceContext[F]): F[StageError] = {
      import context._
      monadError.pure(StageError(originalState.updateStageNo(_ - 1).updateStatus(StageStatus.Finished), error))
    }
  }
}
