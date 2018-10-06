package useless.doobie

import java.util.UUID

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import useless.Journal
import useless.Journal._

class DoobieJournal[F[_]: Monad](transactor: Transactor[F], config: DoobieJournal.Config = DoobieJournal.Config())
    extends Journal[F] {

  import config._

  def persistRawState(state: Journal.RawServiceState): F[Unit] = {
    import state._
    (for {
      exists <- fr"""SELECT $callIDColumn
                     FROM $tableName
                     WHERE $serviceNameColumn = $serviceName
                     AND $callIDColumn = ${callID.toString}""".query[String].option.map(_.isDefined)
      _ <- if (exists) {
        fr"""UPDATE $tableName
             SET $stageNoColumn = $stageNo, $argumentColumn = $argument, $statusColumn = ${status.name}
             WHERE $serviceNameColumn = $serviceName
             AND $callIDColumn = ${callID.toString}""".update.run
      } else {
        fr"""INSERT INTO $tableName ($serviceNameColumn, $callIDColumn, $stageNoColumn, $argumentColumn, $statusColumn)
             VALUES ($serviceName, ${callID.toString}, $stageNo, $argument, ${status.name})""".update.run
      }
    } yield ()).transact(transactor)
  }

  def fetchRawStates(serviceName: String): F[List[Journal.RawServiceState]] =
    fr"""SELECT $callIDColumn, $stageNoColumn, $argumentColumn, $statusColumn
         FROM $tableName
         WHERE $serviceNameColumn = $serviceName"""
      .query[(String, Int, String, String)]
      .to[List]
      .transact(transactor)
      .map(
        _.map {
          case (callId, stageNo, argument, status) =>
            RawServiceState(serviceName, UUID.fromString(callId), stageNo, argument, StageStatus.findByName(status))
        }
      )

  def removeRawStates(callIDs: List[UUID]): F[Unit] =
    fr"""DELETE FROM $tableName WHERE $callIDColumn in (${callIDs.mkString(",")})""".update.run
      .transact(transactor)
      .map(_ => ())
}

object DoobieJournal {

  final case class Config(
    tableName:         String = "journal",
    serviceNameColumn: String = "service_name",
    callIDColumn:      String = "call_id",
    stageNoColumn:     String = "stage_no",
    argumentColumn:    String = "argument",
    statusColumn:      String = "status"
  )
}
