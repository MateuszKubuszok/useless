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
  import profile.api._

  implicit val stageStatusType: JdbcType[StageStatus] with BaseTypedType[StageStatus] =
    MappedColumnType.base[StageStatus, String](_.name, StageStatus.findByName)

  private class JournalEntries(tag: Tag) extends Table[RawServiceState](tag, tableName) {

    val serviceName: Rep[String]      = column[String](serviceNameCol)
    val callID:      Rep[String]      = column[String](callIDCol, O.PrimaryKey)
    val stageNo:     Rep[Int]         = column[Int](stageNoCol)
    val argument:    Rep[String]      = column[String](argumentCol)
    val statusName:  Rep[StageStatus] = column[StageStatus](statusCol)

    private val tupled: ((String, String, Int, String, StageStatus)) => RawServiceState = {
      case (serviceName: String, callID: String, stageNo: Int, argument: String, statusName: StageStatus) =>
        Journal.RawServiceState(serviceName, UUID.fromString(callID), stageNo, argument, statusName)
    }
    private val unapply: RawServiceState => Option[(String, String, Int, String, StageStatus)] = {
      case Journal.RawServiceState(serviceName, callID, stageNo, argument, status) =>
        Some((serviceName, callID.toString, stageNo, argument, status))
    }

    def * : ProvenShape[RawServiceState] = // scalastyle:ignore
      (serviceName, callID, stageNo, argument, statusName) <> (tupled, unapply)
  }

  private val journal = TableQuery[JournalEntries](new JournalEntries(_))

  def persistRawState(state: Journal.RawServiceState): Future[Unit] =
    database.run(journal insertOrUpdate state).map(_ => ())

  def fetchRawStates(serviceName: String): Future[List[Journal.RawServiceState]] =
    database.run(journal.filter(_.serviceName === serviceName).to[List].result)

  def removeRawStates(callIDs: List[UUID]): Future[Unit] =
    database.run(journal.filter(_.callID inSet callIDs.map(_.toString)).delete.map(_ => ()))
}

object SlickJournal {

  final case class Config(
    tableName:      String = "journal",
    serviceNameCol: String = "service_name",
    callIDCol:      String = "call_id",
    stageNoCol:     String = "stage_no",
    argumentCol:    String = "argument",
    statusCol:      String = "status"
  )
}
