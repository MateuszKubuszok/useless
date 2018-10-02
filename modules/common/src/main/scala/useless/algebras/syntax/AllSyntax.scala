package useless.syntax

trait AllSyntax {

  implicit def toMonadErrorOps[F[_], A](fa: F[A]): MonadErrorOps[F, A] = new MonadErrorOps[F, A](fa)
}
