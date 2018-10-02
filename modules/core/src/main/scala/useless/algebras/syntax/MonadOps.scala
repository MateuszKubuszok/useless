package useless.algebras.syntax

import useless.algebras.Monad

class MonadOps[F[_], A](val fa: F[A]) extends AnyVal {

  def flatMap[B](f: A => F[B])(implicit monad: Monad[F]): F[B] = monad.flatMap(fa)(f)

  def map[B](f: A => B)(implicit monad: Monad[F]): F[B] = monad.map(fa)(f)

  def foreach[B](f: A => B)(implicit monad: Monad[F]): Unit = monad.foreach(fa)(f)
}
