package useless.algebras
import scala.concurrent.{ ExecutionContext, Future }

trait Sequence[F[_]] {

  def sequence[A](lfa: List[F[A]]): F[List[A]]
}

object Sequence {

  @inline def apply[F[_]](implicit sequence: Sequence[F]): Sequence[F] = sequence

  implicit def futureTraverse(implicit executionContext: ExecutionContext): Sequence[Future] = new Sequence[Future] {

    def sequence[A](lfa: List[Future[A]]): Future[List[A]] = Future.sequence(lfa)
  }
}
