package useless

import scala.concurrent.{ ExecutionContext, Future }

trait MonadError[F[_], E] {

  def pure[A](value: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def raiseError[A](error: E): F[A]
}

object MonadError {

  implicit def futureMonadError(implicit ec: ExecutionContext): MonadError[Future, Throwable] =
    new MonadError[Future, Throwable] {

      def pure[A](value: A): Future[A] = Future.successful(value)

      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

      def raiseError[A](error: Throwable): Future[A] = Future.failed(error)
    }
}
