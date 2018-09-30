package useless.syntax

import useless.{ PersistentArgument, Stage }

class StageOps[F[_], A, B](val function: A => F[B]) extends AnyVal {

  def retryUntilSucceed(implicit pa: PersistentArgument[A], pb: PersistentArgument[B]): Stage[F, A, B] =
    Stage.RetryUntilSucceed(function)
}
