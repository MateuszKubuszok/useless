package useless

import useless.Journalist._
import useless.internal._
import useless.syntax._

sealed abstract class Stage[F[_], I: PersistentArgument, O: PersistentArgument] {

  private[useless] def runStage(state: ServiceState[I])(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    markStarted(state).flatMap { _ =>
      f(state.argument)
        .flatMap { output =>
          markFinished(state, output)
        }
        .recoverWith { case error: Throwable => recoverStrategy(StageError(state, error)) }
    }
  }

  val f: I => F[O]
  private[useless] def recoverStrategy(error: StageError[I])(implicit context:   ServiceContext[F]): F[ServiceState[O]]
  private[useless] def restoreStage(rawState: RawServiceState)(implicit context: ServiceContext[F]): F[ServiceState[O]]

  protected def markStarted(state: ServiceState[I])(implicit context: ServiceContext[F]): F[Unit] = {
    val newState = state.copy(status = StageStatus.Started)
    context.journalist.persistStatus[I](newState)
  }

  protected def markFinished(state:  ServiceState[I],
                             output: O)(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context.{ serviceName => _, _ }
    import state._
    val newState = ServiceState[O](serviceName, callID, stageNo, output, StageStatus.Finished)
    for {
      _ <- journalist.persistStatus[O](newState)
    } yield newState
  }

  protected def markFailed(stageError: StageError[I])(implicit context: ServiceContext[F]): F[ServiceState[O]] = {
    import context._
    val newState = stageError.previousState.copy(status = StageStatus.Failed)
    journalist.persistStatus[I](newState).flatMap { _ =>
      monadError.raiseError[ServiceState[O]](stageError)
    }
  }
}

object Stage {

  private[useless] final case class RetryUntilSucceed[F[_], I: PersistentArgument, O: PersistentArgument](f: I => F[O])
      extends Stage[F, I, O] {

    private[useless] def recoverStrategy(
      error:            StageError[I]
    )(implicit context: ServiceContext[F]): F[ServiceState[O]] =
      // TODO: rewind stage to previous (set output as current input, stageNo-1)
      ???

    private[useless] def restoreStage(
      rawState:         RawServiceState
    )(implicit context: ServiceContext[F]): F[ServiceState[O]] =
      // TODO
      ???
  }
}
