package useless

import scala.concurrent.{ ExecutionContext, Future }

trait MonadError[F[_], E] {

  def pure[A](value: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))

  def foreach[A, B](fa: F[A])(f: A => B): Unit = { map(fa)(f); () }

  def raiseError[A](error: E): F[A]

  def recover[A](fa: F[A])(f: PartialFunction[E, A]): F[A] = recoverWith(fa) {
    case e if f.isDefinedAt(e) => pure(f(e))
  }

  def recoverWith[A](fa: F[A])(f: PartialFunction[E, F[A]]): F[A]
}

object MonadError {

  @inline def apply[F[_], E](implicit monadError: MonadError[F, E]): MonadError[F, E] = monadError

  implicit def futureMonadError(implicit ec: ExecutionContext): MonadError[Future, Throwable] =
    new MonadError[Future, Throwable] {

      def pure[A](value: A): Future[A] = Future.successful(value)

      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

      def raiseError[A](error: Throwable): Future[A] = Future.failed(error)

      def recoverWith[A](fa: Future[A])(f: PartialFunction[Throwable, Future[A]]): Future[A] = fa.recoverWith(f)
    }
}
