package useless

import java.util.UUID

import useless.Journal._
import useless.algebras.FunctionK
import useless.internal.StageError

trait Journal[F[_]] {

  def persistState[A: PersistentArgument](state: ServiceState[A]): F[Unit] = persistRawState(state.raw)

  def persistRawState(state:      RawServiceState): F[Unit]
  def fetchRawStates(serviceName: String):          F[List[RawServiceState]]
  def removeRawStates(callIDs:    List[UUID]):      F[Unit]

  def mapK[G[_]](fK: FunctionK[F, G]): Journal[G] = new Journal.MappedJournal(this, fK)
}

object Journal {

  final case class ServiceState[A](serviceName: String,
                                   callID:      UUID,
                                   stageNo:     Int,
                                   argument:    A,
                                   status:      StageStatus,
                                   error:       Option[Throwable] = None) {

    def updateArgument[B](newArgument: B): ServiceState[B] =
      ServiceState(serviceName, callID, stageNo, newArgument, status, error)
    def updateStageNo(f:        Int => Int):  ServiceState[A] = copy(stageNo = f(stageNo))
    def updateStatus(newStatus: StageStatus): ServiceState[A] = copy(status  = newStatus)

    def withError(error: Throwable): ServiceState[A] = copy(error = Some(error))
    def cleanError: ServiceState[A] = copy(error = None)

    def raw(implicit pa: PersistentArgument[A]): RawServiceState =
      RawServiceState(serviceName, callID, stageNo, PersistentArgument[A].encode(argument), status)

    def toStageError(implicit pa: PersistentArgument[A]): StageError =
      StageError(this, error.getOrElse(StageError.LostError))

    override def toString: String =
      s"""(serviceName: $serviceName, callID: $callID, stageNo: $stageNo, status: $status)"""
  }
  final case class RawServiceState(serviceName: String,
                                   callID:      UUID,
                                   stageNo:     Int,
                                   argument:    String,
                                   status:      StageStatus) {

    def as[A: PersistentArgument]: ServiceState[A] =
      ServiceState[A](serviceName, callID, stageNo, PersistentArgument[A].decode(argument), status)

    override def toString: String =
      s"""(serviceName: $serviceName, callID: $callID, stageNo: $stageNo, status: $status)"""
  }

  sealed abstract class StageStatus(val name: String) { override def toString: String = name }
  object StageStatus {
    case object Started extends StageStatus("started")
    case object Finished extends StageStatus("finished")
    case object Failed extends StageStatus("failed")
    case object Reverting extends StageStatus("reverting")
    case object IllegalState extends StageStatus("illegal-state")

    val values: Set[StageStatus] = Set(Started, Finished, Failed, Reverting, IllegalState)
    def findByName(name: String): StageStatus = values.find(_.name equalsIgnoreCase name).getOrElse(IllegalState)
  }

  class MappedJournal[F[_], G[_]](journal: Journal[F], fK: FunctionK[F, G]) extends Journal[G] {

    def persistRawState(state:      RawServiceState): G[Unit]                  = fK(journal.persistRawState(state))
    def fetchRawStates(serviceName: String):          G[List[RawServiceState]] = fK(journal.fetchRawStates(serviceName))
    def removeRawStates(callIDs:    List[UUID]):      G[Unit]                  = fK(journal.removeRawStates(callIDs))
  }
}
