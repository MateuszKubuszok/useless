package useless.internal

import java.util.UUID

import useless.Journal.{ RawServiceState, ServiceState, StageStatus }

class ManagedService[F[_], I, O](run: RunAST[F, I, O])(implicit context: ServiceContext[F]) extends (I => F[O]) {

  import context._

  def apply(input: I):               F[O] = findAndRunStage(0, run, Right(initialState(input)))
  def restore(raw: RawServiceState): F[O] = findAndRunStage(0, run, Left(raw))

  private def initialState(input: I): ServiceState[I] =
    ServiceState(serviceName, UUID.randomUUID, 0, input, StageStatus.Finished)

  // pseudocode
  //
  // you have either RawServiceState or ServiceState[I]
  // if you have Left(raw)
  //   if raw.stageNo != current stageNo, then call it recursively, increment stageNo and pass internal runner
  //   else run restored
  // if you have right, it should match stage id so just pass state and run it
  // check result
  //   if ok get nested runner and run it
  //   if failed check status and move up/down until it matches
  private def findAndRunStage[I1, M1](
    currentStageNo: Int,
    currentRun:     RunAST[F, I1, O],
    state:          Either[RawServiceState, ServiceState[I1]]
  ): F[O] = {
    def helper(): Either[RawServiceState, ServiceState[O]] = ???
    printf(
      "%d %d %d %d",
      currentStageNo.hashCode(),
      currentRun.hashCode(),
      state.hashCode(),
      helper().hashCode()
    )
    ???
  }
}
