package useless.algebras.syntax

import useless.algebras.MonadError

class ErrorOps[E](val e: E) extends AnyVal {

  def raiseError[F[_], A](implicit monadError: MonadError[F, E]): F[A] = monadError.raiseError(e)
}
