package useless

import java.util.UUID

import useless.Journalist._

trait Journalist[F[_]] {

  def persistStage[A: PersistentArgument](snapshot: StageSnapshot[A]): F[Unit] = persistRawStage(snapshot.raw)

  def persistRawStage(snapshot:   RawStageSnapshot): F[Unit]
  def fetchRawStages(serviceName: String, states: List[StageState]): F[List[RawStageSnapshot]]
}

object Journalist {

  final case class StageSnapshot[A](serviceName: String,
                                    execution:   UUID,
                                    stageNo:     Int,
                                    argument:    A,
                                    state:       StageState) {

    def raw(implicit pa: PersistentArgument[A]): RawStageSnapshot =
      RawStageSnapshot(serviceName, execution, stageNo, PersistentArgument[A].encode(argument), state)
  }
  final case class RawStageSnapshot(serviceName: String,
                                    execution:   UUID,
                                    stageNo:     Int,
                                    argument:    String,
                                    state:       StageState) {

    def as[A: PersistentArgument]: StageSnapshot[A] =
      StageSnapshot[A](serviceName, execution, stageNo, PersistentArgument[A].decode(argument), state)
  }

  sealed abstract class StageState(val name: String)
  object StageState {
    case object Started extends StageState("started")
    case object Finished extends StageState("finished")

    val values: Set[StageState] = Set(Started, Finished)
    def findByName(name: String): StageState = values.find(_.name equalsIgnoreCase name).getOrElse(Started)
  }

  // FOR TESTING ONLY!
  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private[useless] def inMemory[F[_]](implicit monadError: MonadError[F, Throwable]): Journalist[F] =
    new Journalist[F] {

      import monadError._

      import scala.collection.mutable

      private val storage: mutable.Map[String, mutable.Map[UUID, RawStageSnapshot]] = mutable.Map.empty

      def persistRawStage(snapshot: RawStageSnapshot): F[Unit] =
        map(pure(snapshot)) { s =>
          val snapshots = storage.getOrElseUpdate(s.serviceName, mutable.Map.empty)
          snapshots.update(s.execution, s)
        }

      def fetchRawStages(serviceName: String, states: List[StageState]): F[List[RawStageSnapshot]] =
        map(pure(serviceName -> states)) {
          case (n, s) =>
            storage.get(n).toList.flatMap { snapshots =>
              snapshots.values.toList.filter(snapshot => s.contains(snapshot.state))
            }
        }
    }
}
