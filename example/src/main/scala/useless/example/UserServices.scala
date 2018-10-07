package useless.example

import java.util.UUID

import cats._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import slick.basic.BasicBackend
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

trait UserServices[F[_]] {

  def createUser(user:   User): F[Unit]
  def deleteUser(userID: UUID): F[Unit]
  def fetchUser(userID:  UUID): F[User]
}

class DoobieUserServices[F[_]: MonadError[?[_], Throwable]](transactor: Transactor[F]) extends UserServices[F] {

  private val logger = Logger(getClass)

  def createUser(user: User): F[Unit] = {
    logger.info(s"Creating user: $user")
    fr"""INSERT INTO users (user_id, name, surname)
         VALUES (${user.id}, ${user.name}, ${user.surname})""".update.run.transact(transactor).map(_ => ())
  }
  def deleteUser(userID: UUID): F[Unit] = {
    logger.info(s"Deleting user: $userID")
    fr"""DELETE FROM users WHERE user_id = $userID""".update.run.transact(transactor).map(_ => ())
  }
  def fetchUser(userID: UUID): F[User] = {
    logger.info(s"Fetching user: $userID")
    fr"""SELECT user_id, name, surname
         FROM users
         WHERE user_id = $userID""".query[User].option.transact(transactor).flatMap {
      case Some(user) => user.pure[F]
      case None       => MonadError[F, Throwable].raiseError(new Throwable("No user found"))
    }
  }
}

class SlickUserServices(database: BasicBackend#Database, profile: JdbcProfile)(implicit ec: ExecutionContext)
    extends UserServices[Future] {

  private val logger = Logger(getClass)

  import profile.api._

  private class Users(tag: Tag) extends Table[User](tag, "users") {

    val userID:  Rep[UUID]   = column[UUID]("user_id")
    val name:    Rep[String] = column[String]("name")
    val surname: Rep[String] = column[String]("surname")

    def * : ProvenShape[User] = // scalastyle:ignore
      (userID, name, surname) <> (User.tupled, User.unapply _)
  }

  private val users = TableQuery[Users](new Users(_))

  def createUser(user: User): Future[Unit] = {
    logger.info(s"Creating user: $user")
    database.run(users += user).map(_ => ())
  }
  def deleteUser(userID: UUID): Future[Unit] = {
    logger.info(s"Deleting user: $userID")
    database.run(users.filter(_.userID === userID).delete.map(_ => ()))
  }
  def fetchUser(userID: UUID): Future[User] = {
    logger.info(s"Fetching user: $userID")
    database.run(users.filter(_.userID === userID).result.head)
  }
}
