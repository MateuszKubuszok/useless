package useless.algebras
import scala.concurrent.{ ExecutionContext, Future }

trait Traverse[F[_]] {

  def sequence[A](lfa: List[F[A]]): F[List[A]]
}

object Traverse {

  @inline def apply[F[_]](implicit traverse: Traverse[F]): Traverse[F] = traverse

  implicit def futureTraverse(implicit executionContext: ExecutionContext): Traverse[Future] = new Traverse[Future] {

    def sequence[A](lfa: List[Future[A]]): Future[List[A]] = Future.sequence(lfa)
  }
}
