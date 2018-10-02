package useless

import java.util.UUID

import useless.Journal._
import useless.algebras.MonadError

trait Journal[F[_]] {

  def persistStatus[A: PersistentArgument](state: ServiceState[A]): F[Unit] = persistRawStatus(state.raw)

  def persistRawStatus(state:     RawServiceState): F[Unit]
  def fetchRawStates(serviceName: String):          F[List[RawServiceState]]
  def removeRawStates(callIDs:    List[UUID]):      F[Unit]
}

object Journal {

  final case class ServiceState[A](serviceName: String, callID: UUID, stageNo: Int, argument: A, status: StageStatus) {

    def updateArgument[B](newArgument: B): ServiceState[B] =
      ServiceState(serviceName, callID, stageNo, newArgument, status)
    def updateStageNo(f:        Int => Int):  ServiceState[A] = copy(stageNo = f(stageNo))
    def updateStatus(newStatus: StageStatus): ServiceState[A] = copy(status  = newStatus)

    def raw(implicit pa: PersistentArgument[A]): RawServiceState =
      RawServiceState(serviceName, callID, stageNo, PersistentArgument[A].encode(argument), status)
  }
  final case class RawServiceState(serviceName: String,
                                   callID:      UUID,
                                   stageNo:     Int,
                                   argument:    String,
                                   status:      StageStatus) {

    def as[A: PersistentArgument]: ServiceState[A] =
      ServiceState[A](serviceName, callID, stageNo, PersistentArgument[A].decode(argument), status)
  }

  sealed abstract class StageStatus(val name: String)
  object StageStatus {
    case object Started extends StageStatus("started")
    case object Finished extends StageStatus("finished")
    case object Failed extends StageStatus("failed")
    case object Reverting extends StageStatus("reverting")
    case object IllegalState extends StageStatus("illegal-state")

    val values: Set[StageStatus] = Set(Started, Finished, Failed, Reverting, IllegalState)
    def findByName(name: String): StageStatus = values.find(_.name equalsIgnoreCase name).getOrElse(IllegalState)
  }

  // FOR TESTING ONLY!
  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private[useless] def inMemory[F[_]](implicit monadError: MonadError[F, Throwable]): Journal[F] =
    new Journal[F] {

      import monadError._
      import scala.collection.mutable

      private val storage: mutable.Map[String, mutable.Map[UUID, RawServiceState]] = mutable.Map.empty

      def persistRawStatus(state: RawServiceState): F[Unit] =
        map(pure(state)) { s =>
          storage.getOrElseUpdate(s.serviceName, mutable.Map.empty).update(s.callID, s)
        }

      def fetchRawStates(serviceName: String): F[List[RawServiceState]] =
        map(pure(serviceName)) { n =>
          storage.get(n).toList.flatMap(_.values.toList)
        }

      def removeRawStates(callIDs: List[UUID]): F[Unit] =
        map(pure(callIDs)) { cs =>
          storage.values.foreach { map =>
            callIDs.foreach(map.remove)
          }
        }
    }
}
