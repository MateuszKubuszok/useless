package useless

class StageOps[F[_], A, B](val function: A => F[B]) extends AnyVal {

  def retryUntilSucceed(implicit pa: PersistentArgument[A]): Stage[F, A, B] = Stage.RetryUntilSucceed(function)
}
