package useless.slick

import java.util.UUID

import slick.ast.BaseTypedType
import slick.basic.BasicBackend
import slick.jdbc.{ JdbcProfile, JdbcType }
import slick.lifted.ProvenShape
import useless.Journal
import useless.Journal._

import scala.concurrent.{ ExecutionContext, Future }

class SlickJournal(database: BasicBackend#Database,
                   profile:  JdbcProfile,
                   config:   SlickJournal.Config = SlickJournal.Config())(implicit ec: ExecutionContext)
    extends Journal[Future] {

  import config._
//  import profile._
  import profile.api._

  implicit val stageStatusType: JdbcType[StageStatus] with BaseTypedType[StageStatus] =
    MappedColumnType.base[StageStatus, String](_.name, StageStatus.findByName)

  private class JournalEntries(tag: Tag) extends Table[RawServiceState](tag, tableName) {

    val serviceName: Rep[String]      = column[String](serviceNameColumn)
    val callID:      Rep[UUID]        = column[UUID](callIDColumn)
    val stageNo:     Rep[Int]         = column[Int](stageNoColumn)
    val argument:    Rep[String]      = column[String](argumentColumn)
    val statusName:  Rep[StageStatus] = column[StageStatus](statusColumn)

    def * : ProvenShape[RawServiceState] = // scalastyle:ignore
      (
        serviceName,
        callID,
        stageNo,
        argument,
        statusName
      ) <> (Journal.RawServiceState.tupled, Journal.RawServiceState.unapply _)
  }

  private val journal = TableQuery[JournalEntries](new JournalEntries(_))

  def persistRawState(state: Journal.RawServiceState): Future[Unit] =
    database.run(journal += state).map(_ => ())

  def fetchRawStates(serviceName: String): Future[List[Journal.RawServiceState]] =
    database.run(journal.filter(_.serviceName === serviceName).to[List].result)

  def removeRawStates(callIDs: List[UUID]): Future[Unit] =
    database.run(journal.filter(_.callID inSet callIDs).delete.map(_ => ()))
}

object SlickJournal {

  final case class Config(
    tableName:         String = "journal",
    serviceNameColumn: String = "service_name",
    callIDColumn:      String = "call_id",
    stageNoColumn:     String = "stage_no",
    argumentColumn:    String = "argument",
    statusColumn:      String = "status"
  )
}
