package useless.internal

import useless.Journal.{ RawServiceState, ServiceState, StageStatus }
import useless.PersistentArgument

private[useless] final case class StageError(recoveredState: RawServiceState, error: Throwable) extends Throwable {

  def toServiceState[A: PersistentArgument]: ServiceState[A] = recoveredState.as[A].withError(error)

  override def fillInStackTrace: Throwable = this
}

private[useless] object StageError {

  def apply[I: PersistentArgument](recovered: ServiceState[I], error: Throwable): StageError =
    StageError(recovered.raw, error)

  def onMissingRevert[I: PersistentArgument](state: ServiceState[I]): StageError =
    StageError(state.updateStatus(StageStatus.IllegalState), NonRevertible(state.error.getOrElse(LostError)))

  case object LostError extends Throwable("State was restored - the original exception is lost") {

    override def fillInStackTrace: Throwable = this
  }

  case object BrokenService extends IllegalStateException("Service definition has a flaw that allowed invalid state")

  final case class NonRevertible(error: Throwable)
      extends IllegalStateException("Called revert on non-revertible stage", error) {

    override def fillInStackTrace: Throwable = this
  }
}
