package useless

import useless.internal.{ RunAST, ServiceContext }
import useless.Journal.RawServiceState

import scala.collection.mutable

class Manager[F[_]](journal: Journal[F])(implicit monadError: MonadError[F, Throwable]) {

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private val services: mutable.Map[String, RawServiceState => Any] = mutable.Map.empty

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def apply[A, B](serviceName: String)(process: ProcessBuilder[F, A, B]): A => F[B] = {
    val service = RunAST.fromProcessBuilder(process)(ServiceContext(serviceName, journal, monadError))
    if (services.get(serviceName).isDefined) throw new RuntimeException(s"Service name $serviceName already taken!")
    else services.update(serviceName, service.restore)
    service
  }

  // TODO: retrieve
}

object Manager {

  def apply[F[_]](journal: Journal[F])(implicit monadError: MonadError[F, Throwable]): Manager[F] =
    new Manager[F](journal)
}
