package useless.example

import java.util.UUID

import cats.effect._
import com.typesafe.scalalogging.Logger
import doobie._
import org.flywaydb.core.Flyway
import slick.basic.{ BasicBackend, DatabaseConfig }
import useless._
import useless.algebras.MonadError
import useless.cats._ // allows turning cats.MonadError into useless.algebra.MonadError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

object Example {

  private val logger = Logger(getClass)

  // required by Doobie
  implicit val catsContextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val catsTimer:        Timer[IO]        = IO.timer(global)
  lazy val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${sys.env("PGHOST")}:${sys.env("PGPORT")}/${sys.env("PGDATABASE")}",
    sys.env("PGUSER"),
    sys.env("PGPASSWORD")
  )

  // Journal implemented using Doobie components
  lazy val doobieJournal: Journal[IO] = new useless.doobie.DoobieJournal[IO](transactor)
  // Services implemented using Doobie components
  implicit lazy val doobieEntitlementServices: EntitlementServices[IO] = new DoobieEntitlementServices[IO](transactor)
  implicit lazy val doobieUserServices:        UserServices[IO]        = new DoobieUserServices[IO](transactor)

  // required by Slick
  lazy val slickDatabase: BasicBackend#Database = DatabaseConfig.forConfig[SlickProfile]("database").db

  // Journal implemented using Slick components
  lazy val slickJournal: Journal[Future] = new useless.slick.SlickJournal(slickDatabase, SlickProfile)
  // Services implemented using Slick components
  implicit lazy val slickEntitlementServices: EntitlementServices[Future] =
    new SlickEntitlementServices(slickDatabase, SlickProfile)
  implicit lazy val slickUserServices: UserServices[Future] = new SlickUserServices(slickDatabase, SlickProfile)

  def main(args: Array[String]): Unit = {
    runMigration()
    if (args.contains("doobie")) {
      implicit val manager = Manager[IO](doobieJournal, (msg: String) => logger.info(msg))
      runTest[IO].unsafeRunSync()
    } else if (args.contains("slick")) {
      implicit val manager = Manager[Future](slickJournal, (msg: String) => logger.info(msg))
      Await.result(runTest[Future], Duration.Inf)
    }
  }

  def runMigration(): Unit = {
    Flyway
      .configure()
      .dataSource(
        s"jdbc:postgresql://${sys.env("PGHOST")}:${sys.env("PGPORT")}/${sys.env("PGDATABASE")}",
        sys.env("PGUSER"),
        sys.env("PGPASSWORD")
      )
      .load()
      .migrate()
    ()
  }

  def runTest[F[_]: Manager: MonadError[?[_], Throwable]: EntitlementServices: UserServices]: F[Unit] =
    new AdminServices[F](implicitly[UserServices[F]], implicitly[EntitlementServices[F]])
      .createAdminForResource(User(UUID.randomUUID, "John", "Smith"), UUID.randomUUID)
}
