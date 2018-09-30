package useless

import useless.Journalist._

trait Journalist[F[_]] {

  def persistStage[A: PersistentArgument](snapshot: StageSnapshot[A]): F[Unit] = persistRawStage(snapshot.raw)

  def persistRawStage(snapshot:   RawStageSnapshot): F[Unit]
  def fetchRawStages(serviceName: String, states: List[StageState]): F[List[RawStageSnapshot]]
}

object Journalist {

  final case class StageSnapshot[A](serviceName: String, stageNo: Int, argument: A, state: StageState) {

    def raw(implicit pa: PersistentArgument[A]): RawStageSnapshot =
      RawStageSnapshot(serviceName, stageNo, pa.encode(argument), state.name)
  }
  final case class RawStageSnapshot(serviceName: String, stageNo: Int, argument: String, state: String) {

    def as[A: PersistentArgument]: StageSnapshot[A] =
      StageSnapshot[A](serviceName, stageNo, PersistentArgument[A].decode(argument), StageState.findByName(state))
  }

  sealed abstract class StageState(val name: String)
  object StageState {
    case object Started extends StageState("started")
    case object Finished extends StageState("finished")

    val values: Set[StageState] = Set(Started, Finished)
    def findByName(name: String): StageState = values.find(_.name equalsIgnoreCase name).getOrElse(Started)
  }
}
