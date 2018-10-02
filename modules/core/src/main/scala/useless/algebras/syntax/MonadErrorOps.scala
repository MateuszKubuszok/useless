package useless.algebras.syntax

import useless.algebras.MonadError

class MonadErrorOps[F[_], A](val fa: F[A]) extends AnyVal {

  def recover[E](f: PartialFunction[E, A])(implicit monadError: MonadError[F, E]): F[A] =
    monadError.recover(fa)(f)

  def recoverWith[E](f: PartialFunction[E, F[A]])(implicit monadError: MonadError[F, E]): F[A] =
    monadError.recoverWith(fa)(f)
}
