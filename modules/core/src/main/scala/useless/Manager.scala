package useless

import useless.Journal.RawServiceState
import useless.internal.{ RunAST, ServiceContext }
import useless.algebras._
import useless.algebras.syntax._
import useless.Manager.RetryResult

import scala.collection.mutable

class Manager[F[_]: MonadThrowable: Traverse](journal: Journal[F], logger: String => Unit) {

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private val services: mutable.Map[String, RawServiceState => F[_]] = mutable.Map.empty

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def apply[A, B](serviceName: String)(process: ProcessBuilder[F, A, B]): A => F[B] = {
    val context = ServiceContext(serviceName, journal, logger, MonadError[F, Throwable])
    val service = RunAST.fromProcessBuilder(process)(context)
    if (services.get(serviceName).isDefined) throw new RuntimeException(s"Service name $serviceName already taken!")
    else services.update(serviceName, service.restore)
    service
  }

  def retryServicesInDB(): F[List[RetryResult]] =
    services.toList.foldLeft(List.empty[RetryResult].pure[F]) {
      case (resultF, (serviceName, restore)) =>
        val restoredF =
          Monad[F].flatten[List[RetryResult]](
            journal
              .fetchRawStates(serviceName)
              .map(_.map(restore(_).map(_.asInstanceOf[Any]).toAttempt[Throwable]))
              .map(Traverse[F].sequence)
          )

        for {
          result <- resultF
          restored <- restoredF
        } yield result ++ restored
    }
}

object Manager {

  type RetryResult = Either[Throwable, Any]

  def apply[F[_]: MonadThrowable: Traverse](journal: Journal[F]): Manager[F] =
    new Manager[F](journal, _ => ())

  def apply[F[_]: MonadThrowable: Traverse](journal: Journal[F], logger: String => Unit): Manager[F] =
    new Manager[F](journal, logger)
}
