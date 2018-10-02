package useless.internal

import java.util.UUID

import useless.Journal.{ RawServiceState, ServiceState, StageStatus }
import useless.algebras.syntax._

class ManagedService[F[_], I, O](run: RunAST[F, I, O])(implicit context: ServiceContext[F]) extends (I => F[O]) {

  import context._

  def apply(input: I):               F[O] = findAndRunStage(0, run, Right(initialState(input)))
  def restore(raw: RawServiceState): F[O] = findAndRunStage(0, run, Left(raw))

  private type RestorableState[A] = Either[RawServiceState, ServiceState[A]]

  private def initialState(input: I): ServiceState[I] =
    ServiceState(serviceName, UUID.randomUUID, 0, input, StageStatus.Finished)

  // scalastyle:off
  @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Recursion"))
  private def findAndRunStage[I1, M1](
    currentStageNo:  Int,
    currentRun:      RunAST[F, I1, O],
    restorableState: RestorableState[I1]
  ): F[O] =
    ((restorableState, currentRun) match {
      // terminate on illegal state
      case (Left(rawState), _) if rawState.status == StageStatus.IllegalState =>
        monadError.raiseError[O](StageError.BrokenService)
      // rewind to right state
      case (Left(rawState), RunAST.Proceed(_, next)) if currentStageNo < rawState.stageNo =>
        findAndRunStage(currentStageNo + 1, next, Left(rawState))
      // restore from raw state
      case (Left(rawState), RunAST.Proceed(current, next)) =>
        current.restoreStage(rawState).map(Right(_)).flatMap(findAndRunStage(currentStageNo + 1, next, _))
      // terminate on illegal state
      case (Right(state), _) if state.status == StageStatus.IllegalState =>
        monadError.raiseError[O](StageError.BrokenService)
      // run stage normally
      case (Right(state), RunAST.Proceed(current, next)) =>
        current.runStage(state).map(Right(_)).flatMap(findAndRunStage(currentStageNo + 1, next, _))
      // reached end of the process
      case (Right(state), RunAST.Stop()) =>
        journal.removeRawStates(List(state.callID)).map(_ => state.argument.asInstanceOf[O])
      case _ => monadError.raiseError[O](StageError.BrokenService)
    }).recoverWith[Throwable] {
      case StageError(rawState, _) if rawState.status != StageStatus.IllegalState && rawState.stageNo > 0 =>
        restore(rawState)
      case StageError(_, error) =>
        monadError.raiseError[O](error)
    }
  // scalastyle:on
}
