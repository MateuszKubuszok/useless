package useless

import useless.internal.{ RunAST, ServiceContext }
import useless.Journal.RawServiceState
import useless.algebras.{ MonadError, MonadThrowable }

import scala.collection.mutable

class Manager[F[_]: MonadThrowable](journal: Journal[F], logger: String => Unit) {

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private val services: mutable.Map[String, RawServiceState => Any] = mutable.Map.empty

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def apply[A, B](serviceName: String)(process: ProcessBuilder[F, A, B]): A => F[B] = {
    val context = ServiceContext(serviceName, journal, logger, MonadError[F, Throwable])
    val service = RunAST.fromProcessBuilder(process)(context)
    if (services.get(serviceName).isDefined) throw new RuntimeException(s"Service name $serviceName already taken!")
    else services.update(serviceName, service.restore)
    service
  }

  // TODO: retrieve unfinished and restore
}

object Manager {

  def apply[F[_]: MonadThrowable](journal: Journal[F]): Manager[F] =
    new Manager[F](journal, _ => ())

  def apply[F[_]: MonadThrowable](journal: Journal[F], logger: String => Unit): Manager[F] =
    new Manager[F](journal, logger)
}
