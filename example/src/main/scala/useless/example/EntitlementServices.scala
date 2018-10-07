package useless.example

import java.util.UUID

import cats._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import slick.ast.BaseTypedType
import slick.basic.BasicBackend
import slick.jdbc.{ JdbcProfile, JdbcType }
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

trait EntitlementServices[F[_]] {

  def createEntitlement(entitlement:  Entitlement): F[Unit]
  def deleteEntitlement(entitlement:  Entitlement): F[Unit]
  def fetchEntitlementForUser(userID: UUID):        F[Set[Entitlement]]
}

class DoobieEntitlementServices[F[_]: MonadError[?[_], Throwable]](transactor: Transactor[F])
    extends EntitlementServices[F] {

  private val logger = Logger(getClass)

  def createEntitlement(entitlement: Entitlement): F[Unit] = {
    logger.info(s"Creating entitlement: $entitlement")
    fr"""INSERT INTO entitlements (user_id, resource_id, level)
         VALUES (${entitlement.userID}, ${entitlement.resourceID}, ${entitlement.level.name})""".update.run
      .transact(transactor)
      .map(_ => ())
  }
  def deleteEntitlement(entitlement: Entitlement): F[Unit] = {
    logger.info(s"Deleting entitlement: $entitlement")
    fr"""DELETE FROM entitlements
         WHERE user_id = ${entitlement.userID}
         AND resource_id = ${entitlement.resourceID}
         AND level = ${entitlement.level.name}""".update.run.transact(transactor).map(_ => ())
  }
  def fetchEntitlementForUser(userID: UUID): F[Set[Entitlement]] = {
    logger.info(s"Fetching entitlements for user: $userID")
    fr"""SELECT resource_id, level
         FROM users
         WHERE user_id = $userID"""
      .query[(UUID, String)]
      .map {
        case (resourceID, level) =>
          Entitlement(userID, resourceID, Entitlement.Level.findByName(level))
      }
      .to[Set]
      .transact(transactor)
  }
}

class SlickEntitlementServices(database: BasicBackend#Database, profile: JdbcProfile)(implicit ec: ExecutionContext)
    extends EntitlementServices[Future] {

  private val logger = Logger(getClass)

  import profile.api._

  implicit val entitlementLevelType: JdbcType[Entitlement.Level] with BaseTypedType[Entitlement.Level] =
    MappedColumnType.base[Entitlement.Level, String](_.name, Entitlement.Level.findByName)

  private class Entitlements(tag: Tag) extends Table[Entitlement](tag, "entitlements") {

    val userID:     Rep[UUID]              = column[UUID]("user_id")
    val resourceID: Rep[UUID]              = column[UUID]("resource_id")
    val level:      Rep[Entitlement.Level] = column[Entitlement.Level]("level")

    def * : ProvenShape[Entitlement] = // scalastyle:ignore
      (userID, resourceID, level) <> ((Entitlement.apply _).tupled, Entitlement.unapply _)
  }

  private val entitlements = TableQuery[Entitlements](new Entitlements(_))

  def createEntitlement(entitlement: Entitlement): Future[Unit] = {
    logger.info(s"Creating entitlement: $entitlement")
    database.run(entitlements += entitlement).map(_ => ())
  }
  def deleteEntitlement(entitlement: Entitlement): Future[Unit] = {
    logger.info(s"Deleting entitlement: $entitlement")
    database
      .run(
        entitlements.filter { e =>
          (e.userID === entitlement.userID) &&
          (e.resourceID === entitlement.resourceID) &&
          (e.level === entitlement.level)
        }.delete
      )
      .map(_ => ())
  }
  def fetchEntitlementForUser(userID: UUID): Future[Set[Entitlement]] = {
    logger.info(s"Fetching entitlements for user: $userID")
    database.run(entitlements.filter(_.userID === userID).to[Set].result)
  }
}
