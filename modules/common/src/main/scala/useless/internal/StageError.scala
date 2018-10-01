package useless.internal

import useless.Journal.{ RawServiceState, ServiceState, StageStatus }
import useless.PersistentArgument

private[useless] final case class StageError(recoveredState: RawServiceState, error: Throwable) extends Throwable {

  override def fillInStackTrace: Throwable = this
}

private[useless] object StageError {

  def apply[I: PersistentArgument](recovered: ServiceState[I], error: Throwable): StageError =
    StageError(recovered.raw, error)

  def onIllegalState[I: PersistentArgument](state: ServiceState[I]): StageError =
    StageError(state.updateStatus(StageStatus.IllegalState), NonRevertible)

  case object Restored extends Throwable("Stage restored from journal - the original exception is lost") {

    override def fillInStackTrace: Throwable = this
  }

  case object NonRevertible extends IllegalStateException("Called revert on non-revertible stage") {

    override def fillInStackTrace: Throwable = this
  }
}
