package useless.extra

import useless.{ PersistentArgument, ProcessBuilder, ReversibleProcessBuilder }
import useless.algebras.{ Monad, MonadThrowable, Timer }
import useless.algebras.syntax._
import useless.ProcessBuilder.{ CustomStrategy, OnError }
import useless.ProcessBuilder.OnError.{ Retry, Revert }

import scala.concurrent.duration.FiniteDuration

object BoundedRetry {

  def build[F[_]: Monad, I, O: PersistentArgument, O2: PersistentArgument](
    rpb:                 ReversibleProcessBuilder[F, I, O],
    run:                 O => F[O2],
    revert:              O2 => F[O],
    maxAttempts:         Int,
    initialDelay:        FiniteDuration,
    delayIncreaseFactor: Int
  ): ReversibleProcessBuilder[F, I, O2] =
    rpb
      .retryUntilSucceed[Argument[O]](wrap[F, O])(unwrap[F, O])
      .customHandler[O2](o => unwrap[F, O].apply(o).flatMap(run))(o2 => revert(o2).flatMap(wrap[F, O]))(
        new Strategy(maxAttempts, initialDelay, delayIncreaseFactor)
      )

  def wrap[F[_]:   Monad, I]: I => F[Argument[I]] = input => Argument(0, input).pure[F]
  def unwrap[F[_]: Monad, I]: Argument[I] => F[I] = input => input.argument.pure[F]

  final case class Argument[A](attempt: Int, argument: A) {

    def incrementAttempts: Argument[A] = copy(attempt + 1)
  }

  object Argument {

    implicit def persistentArgument[A: PersistentArgument]: PersistentArgument[Argument[A]] =
      new PersistentArgument[Argument[A]] {

        def encode(value: Argument[A]): String =
          s"${value.attempt};${PersistentArgument[A].encode(value.argument)}"
        def decode(value: String): Argument[A] = {
          val (attemptStr, semiArgument) = value.splitAt(value.indexOf(";"))
          Argument[A](attemptStr.toInt, PersistentArgument[A].decode(semiArgument.drop(1)))
        }
      }
  }

  class Strategy(maxAttempts: Int, initialDelay: FiniteDuration, delayIncreaseFactor: Int) extends CustomStrategy {

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def apply[F[_]: MonadThrowable: Timer, I: PersistentArgument](argument: I): F[(I, ProcessBuilder.OnError)] =
      argument match {
        case bra: Argument[_] =>
          val newArg = bra.incrementAttempts
          if (newArg.attempt > maxAttempts) (newArg.asInstanceOf[I], Revert:                           OnError).pure[F]
          else Timer[F].sleep(calculateDelay(newArg.attempt)).map(_ => (newArg.asInstanceOf[I], Retry: OnError))
        case _ => (argument, Revert: OnError).pure[F]
      }

    private def calculateDelay(attempt: Int): FiniteDuration =
      (0 until (attempt - 1)).foldLeft(initialDelay)((delay, _) => delay * delayIncreaseFactor.toLong)
  }
}
