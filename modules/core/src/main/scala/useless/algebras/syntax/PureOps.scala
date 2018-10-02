package useless.algebras.syntax

import useless.algebras.Monad

class PureOps[A](val a: A) extends AnyVal {

  def pure[F[_]](implicit monad: Monad[F]): F[A] = monad.pure(a)
}
