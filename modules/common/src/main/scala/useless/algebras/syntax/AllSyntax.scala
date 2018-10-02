package useless.algebras.syntax

trait AllSyntax {

  implicit def toPureOps[A](a: A): PureOps[A] = new PureOps[A](a)

  implicit def toErrorOps[E](e: E): ErrorOps[E] = new ErrorOps[E](e)

  implicit def toMonadOps[F[_], A](fa: F[A]): MonadOps[F, A] = new MonadOps[F, A](fa)

  implicit def toMonadErrorOps[F[_], A](fa: F[A]): MonadErrorOps[F, A] = new MonadErrorOps[F, A](fa)
}
