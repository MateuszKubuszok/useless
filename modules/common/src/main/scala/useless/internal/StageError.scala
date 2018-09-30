package useless.internal
import useless.Journalist.ServiceState

final case class StageError[I](previousState: ServiceState[I], error: Throwable) extends Throwable {

  override def fillInStackTrace: Throwable = this
}
