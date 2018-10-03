package useless.internal

import java.util.UUID

import useless.Journal.{ RawServiceState, ServiceState, StageStatus }
import useless.algebras.syntax._
import useless.Journal.StageStatus._

class ManagedService[F[_], I, O](run: RunAST[F, I, O])(implicit context: ServiceContext[F]) extends (I => F[O]) {

  import context._

  def apply(input: I):               F[O] = findAndRunStage(1, run, Right(initialState(input)))
  def restore(raw: RawServiceState): F[O] = restore(raw, StageError.LostError)

  private def restore(raw: RawServiceState, error: Throwable): F[O] =
    findAndRunStage(1, run, Left(StageError(raw, error)))

  private type RestorableState[A] = Either[StageError, ServiceState[A]]

  private def initialState(input: I): ServiceState[I] = ServiceState(serviceName, UUID.randomUUID, 0, input, Finished)

  // scalastyle:off
  @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Recursion"))
  private def findAndRunStage[I1, M1](
    currentStageNo:  Int,
    currentRun:      RunAST[F, I1, O],
    restorableState: RestorableState[I1]
  ): F[O] =
    ((restorableState, currentRun) match {
      case (Left(StageError(rawState, error)), _) if rawState.status == IllegalState =>
        logger(s"""terminate on illegal state
                  |  raw state : $rawState
                """.stripMargin)
        monadError.raiseError[O](error)
      case (Left(StageError(rawState, error)), RunAST.Proceed(_, next)) if currentStageNo < rawState.stageNo =>
        logger(
          s"""rewind to right stage
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  raw state        : $rawState
           """.stripMargin
        )
        findAndRunStage(currentStageNo + 1, next, Left(StageError(rawState, error)))
      case (Left(StageError(rawState, error)), _) if rawState.status == Reverting && rawState.stageNo <= 0 =>
        logger(s"""restoring dead state
                  |  raw state : $rawState
                """.stripMargin)
        monadError.raiseError[O](error)
      case (Left(StageError(rawState, error)), RunAST.Proceed(current, next)) if rawState.status == Reverting =>
        logger(
          s"""found right stage, reverting
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  raw state        : $rawState
           """.stripMargin
        )
        current
          .runRevert(StageError(rawState, error))
          .map(Right(_))
          .flatMap(findAndRunStage(currentStageNo + 1, next, _))
      case (Left(StageError(rawState, error)), RunAST.Proceed(current, next)) =>
        logger(
          s"""found right stage, running
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  raw state        : $rawState
           """.stripMargin
        )
        current.restoreStage(rawState, Some(error)).map(Right(_)).flatMap(findAndRunStage(currentStageNo + 1, next, _))
      case (Right(state), _) if state.status == IllegalState =>
        logger(s"""terminate on illegal state
                  |  state : $state
                """.stripMargin)
        monadError.raiseError[O](StageError.BrokenService)
      case (Right(state), _) if state.status == StageStatus.Finished =>
        logger(
          s"""proceed to next stage
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  state            : $state
           """.stripMargin
        )
        findAndRunStage(currentStageNo, currentRun, Right(state.updateStageNo(_ + 1).updateStatus(StageStatus.Started)))
      case (Right(state), RunAST.Proceed(current, next)) =>
        logger(
          s"""running
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  state            : $state
           """.stripMargin
        )
        current.runStage(state).map(Right(_)).flatMap(findAndRunStage(currentStageNo + 1, next, _))
      case (Right(state), RunAST.Stop()) =>
        logger(s"""finished running
                  |  state : $state
                """.stripMargin)
        journal.removeRawStates(List(state.callID)).map(_ => state.argument.asInstanceOf[O])
      case state =>
        logger(
          s"""unexpected state
             |  current stage no : $currentStageNo
             |  current run      : $currentRun
             |  state            : $state
           """.stripMargin
        )
        monadError.raiseError[O](StageError.BrokenService)
    }).recoverWith[Throwable] {
      case StageError(rawState, error) if rawState.status != IllegalState && rawState.stageNo >= 0 =>
        logger(s"""restoring from state
                  |  raw state : $rawState"
                """.stripMargin)
        restore(rawState, error)
      case StageError(state, error) =>
        logger(
          s"""failing with error
             |  state : $state
             |  error : $error
           """.stripMargin
        )
        monadError.raiseError[O](error)
    }
  // scalastyle:on
}
