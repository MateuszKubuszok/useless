package useless.example

import java.util.UUID

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import slick.basic.BasicBackend
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

trait UserServices[F[_]] {

  def createUser(user:   User): F[Unit]
  def deleteUser(userID: UUID): F[Unit]
  def fetchUser(userID:  UUID): F[User]
}

class DoobieUserServices[F[_]: ({ type L[A[_]] = MonadError[A, Throwable] })#L]( // scalastyle:ignore
  transactor: Transactor[F]
) extends UserServices[F] {

  def createUser(user: User): F[Unit] =
    fr"""INSERT INTO users (user_id, name, surname)
         VALUES (${user.id.toString}, ${user.name}, ${user.surname})""".update.run.transact(transactor).map(_ => ())
  def deleteUser(userID: UUID): F[Unit] =
    fr"""DELETE FROM users WHERE user_id = ${userID.toString})""".update.run.transact(transactor).map(_ => ())
  def fetchUser(userID: UUID): F[User] =
    fr"""SELECT call_id, name, surname
         FROM users
         WHERE user_id = ${userID.toString}"""
      .query[(String, String, String)]
      .map {
        case (id, name, surname) =>
          User(UUID.fromString(id), name, surname)
      }
      .option
      .transact(transactor)
      .flatMap {
        case Some(user) => user.pure[F]
        case None       => MonadError[F, Throwable].raiseError(new Throwable("No user found"))
      }
}

class SlickUserServices(database: BasicBackend#Database, profile: JdbcProfile)(implicit ec: ExecutionContext)
    extends UserServices[Future] {

  import profile.api._

  private class Users(tag: Tag) extends Table[User](tag, "users") {

    val userID:  Rep[UUID]   = column[UUID]("user_id")
    val name:    Rep[String] = column[String]("name")
    val surname: Rep[String] = column[String]("surname")

    def * : ProvenShape[User] = // scalastyle:ignore
      (userID, name, surname) <> (User.tupled, User.unapply _)
  }

  private val users = TableQuery[Users](new Users(_))

  def createUser(user: User): Future[Unit] =
    database.run(users += user).map(_ => ())
  def deleteUser(userID: UUID): Future[Unit] =
    database.run(users.filter(_.userID === userID).delete.map(_ => ()))
  def fetchUser(userID: UUID): Future[User] =
    database.run(users.filter(_.userID === userID).result.head)
}
