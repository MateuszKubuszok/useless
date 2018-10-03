package useless

import useless.Journal.RawServiceState
import useless.internal.{ RunAST, ServiceContext }
import useless.algebras._
import useless.algebras.syntax._
import useless.Manager.RetryResult

import scala.collection.mutable

class Manager[F[_]: MonadThrowable: Sequence](journal: Journal[F], logger: String => Unit) {

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

  def resumeInterruptedServices(): F[Unit] =
    Sequence[F]
      .sequence(
        services.toList.map {
          case (serviceName, restore) =>
            Monad[F].flatten[Unit](
              journal
                .fetchRawStates(serviceName)
                .map(_.map(restore(_).map(_ => ()).recover[Throwable] { case _ => () }))
                .map(Sequence[F].sequence(_).map(_ => ()))
            )
        }
      )
      .map(_ => ())

  private[useless] def resumeInterruptedServicesUnsafe(): F[List[RetryResult]] =
    Sequence[F]
      .sequence(
        services.toList.map {
          case (serviceName, restore) =>
            Monad[F].flatten[List[RetryResult]](
              journal
                .fetchRawStates(serviceName)
                .map(_.map(restore(_).map(_.asInstanceOf[Any]).toAttempt[Throwable]))
                .map(Sequence[F].sequence)
            )
        }
      )
      .map(_.flatten)
}

object Manager {

  type RetryResult = Either[Throwable, Any]

  def apply[F[_]: MonadThrowable: Sequence](journal: Journal[F]): Manager[F] =
    new Manager[F](journal, _ => ())

  def apply[F[_]: MonadThrowable: Sequence](journal: Journal[F], logger: String => Unit): Manager[F] =
    new Manager[F](journal, logger)
}
