package useless.algebras
import scala.concurrent.{ ExecutionContext, Future }

trait Monad[F[_]] {

  def pure[A](value: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def flatten[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(fa => fa)

  def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))

  def foreach[A, B](fa: F[A])(f: A => B): Unit = { map(fa)(f); () }
}

object Monad {

  @inline def apply[F[_]](implicit monad: Monad[F]): Monad[F] = monad

  implicit def futureMonadError(implicit ec: ExecutionContext): Monad[Future] =
    MonadError.futureMonadError
}
