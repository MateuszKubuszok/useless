package useless.algebras

import scala.concurrent._
import scala.concurrent.duration._

trait Timer[F[_]] {

  def sleep(duration: FiniteDuration): F[_]
}

object Timer {

  @inline def apply[F[_]](implicit timer: Timer[F]): Timer[F] = timer

  implicit def futureTimer(implicit ec: ExecutionContext): Timer[Future] =
    (duration: FiniteDuration) =>
      Future.unit.flatMap(_ => blocking(Await.result(Future.never, duration))).recoverWith { case _ => Future.unit }
}
