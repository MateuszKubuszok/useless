package useless.extra
import useless.{ PersistentArgument, ReversibleProcessBuilder }
import useless.algebras.Monad

import scala.concurrent.duration.FiniteDuration

class BoundedRetryOps[F[_], I, O](val rbp: ReversibleProcessBuilder[F, I, O]) extends AnyVal {

  def retryWithBounds[O2](run: O => F[O2])(revert: O2 => F[O])(
    maxAttempts:               Int,
    initialDelay:              FiniteDuration,
    delayIncreaseFactor:       Int
  )(
    implicit monad: Monad[F],
    po:             PersistentArgument[O],
    po2:            PersistentArgument[O2],
  ): ReversibleProcessBuilder[F, I, O2] =
    BoundedRetry.build(rbp, run, revert, maxAttempts, initialDelay, delayIncreaseFactor)
}
