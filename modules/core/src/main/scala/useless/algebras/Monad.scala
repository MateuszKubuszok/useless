package useless.algebras

trait Monad[F[_]] {

  def pure[A](value: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))

  def foreach[A, B](fa: F[A])(f: A => B): Unit = { map(fa)(f); () }
}

object Monad {

  @inline def apply[F[_], E](implicit monadError: MonadError[F, E]): MonadError[F, E] = monadError
}
