package useless.syntax

trait AllSyntax {

  implicit def toMonadErrorOps[F[_], A](fa: F[A]): MonadErrorOps[F, A] = new MonadErrorOps[F, A](fa)

  implicit def toStageOps[F[_], A, B](f: A => F[B]): StageOps[F, A, B] = new StageOps(f)
}
