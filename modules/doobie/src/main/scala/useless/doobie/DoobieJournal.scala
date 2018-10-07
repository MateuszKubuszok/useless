package useless.doobie

import java.util.UUID

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.fragment.Fragment.const
import useless.Journal
import useless.Journal._

class DoobieJournal[F[_]: Monad](transactor: Transactor[F], config: DoobieJournal.Config = DoobieJournal.Config())
    extends Journal[F] {

  import config._

  def persistRawState(state: Journal.RawServiceState): F[Unit] = {
    import state._
    (for {
      exists <- (fr"SELECT" ++ const(callIDCol) ++
        fr"FROM" ++ const(tableName) ++
        fr"WHERE" ++ const(callIDCol) ++ fr" = ${callID.toString}").query[String].option.map(_.isDefined)
      _ <- if (exists) {
        (fr"UPDATE" ++ const(tableName) ++
          fr"SET" ++ const(stageNoCol) ++ fr"= $stageNo," ++
          const(argumentCol) ++ fr" = $argument," ++
          const(statusCol) ++ fr" = ${status.name}" ++
          fr"WHERE" ++ const(serviceNameCol) ++ fr" = $serviceName" ++
          fr"AND" ++ const(callIDCol) ++ fr"= ${callID.toString}").update.run
      } else {
        (fr"INSERT INTO" ++ const(tableName) ++
          fr"(" ++ const(serviceNameCol) ++ fr"," ++ const(callIDCol) ++ fr"," ++ const(stageNoCol) ++ fr"," ++
          const(argumentCol) ++ fr"," ++ const(statusCol) ++
          fr")" ++
          fr"VALUES ($serviceName, ${callID.toString}, $stageNo, $argument, ${status.name})").update.run
      }
    } yield ()).transact(transactor)
  }

  def fetchRawStates(serviceName: String): F[List[Journal.RawServiceState]] =
    (fr"SELECT" ++ const(callIDCol) ++ fr"," ++ const(stageNoCol) ++ fr"," ++ const(argumentCol) ++ fr"," ++ const(
      statusCol
    ) ++
      fr"FROM" ++ const(tableName) ++
      fr"WHERE" ++ const(serviceNameCol) ++ fr" = $serviceName")
      .query[(String, Int, String, String)]
      .to[List]
      .transact(transactor)
      .map(
        _.map {
          case (callId, stageNo, argument, status) =>
            RawServiceState(serviceName, UUID.fromString(callId), stageNo, argument, StageStatus.findByName(status))
        }
      )

  def removeRawStates(callIDs: List[UUID]): F[Unit] = {
    val head :: tail = callIDs
    (fr"DELETE FROM" ++ const(tableName) ++ fr"WHERE" ++ Fragments.in(
      Fragment.const(callIDCol),
      NonEmptyList(head, tail).map(_.toString)
    )).update.run.transact(transactor).map(_ => ())
  }
}

object DoobieJournal {

  final case class Config(
    tableName:      String = "journal",
    serviceNameCol: String = "service_name",
    callIDCol:      String = "call_id",
    stageNoCol:     String = "stage_no",
    argumentCol:    String = "argument",
    statusCol:      String = "status"
  )
}
