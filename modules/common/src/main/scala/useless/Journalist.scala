package useless

import java.util.UUID

import useless.Journalist._

trait Journalist[F[_]] {

  def persistStatus[A: PersistentArgument](snapshot: ServiceState[A]): F[Unit] = persistRawStatus(snapshot.raw)

  def persistRawStatus(snapshot:  RawServiceState): F[Unit]
  def fetchRawStages(serviceName: String, states: List[StageStatus]): F[List[RawServiceState]]
}

object Journalist {

  final case class ServiceState[A](serviceName: String, callID: UUID, stageNo: Int, argument: A, status: StageStatus) {

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

    val values: Set[StageStatus] = Set(Started, Finished)
    def findByName(name: String): StageStatus = values.find(_.name equalsIgnoreCase name).getOrElse(Failed)
  }

  // FOR TESTING ONLY!
  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private[useless] def inMemory[F[_]](implicit monadError: MonadError[F, Throwable]): Journalist[F] =
    new Journalist[F] {

      import monadError._

      import scala.collection.mutable

      private val storage: mutable.Map[String, mutable.Map[UUID, RawServiceState]] = mutable.Map.empty

      def persistRawStatus(snapshot: RawServiceState): F[Unit] =
        map(pure(snapshot)) { s =>
          val snapshots = storage.getOrElseUpdate(s.serviceName, mutable.Map.empty)
          snapshots.update(s.callID, s)
        }

      def fetchRawStages(serviceName: String, states: List[StageStatus]): F[List[RawServiceState]] =
        map(pure(serviceName -> states)) {
          case (n, s) =>
            storage.get(n).toList.flatMap { snapshots =>
              snapshots.values.toList.filter(snapshot => s.contains(snapshot.status))
            }
        }
    }
}
