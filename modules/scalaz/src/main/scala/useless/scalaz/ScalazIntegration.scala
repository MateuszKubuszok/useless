package useless.scalaz

import scalaz.std.list._

import scala.concurrent.duration.FiniteDuration

trait ScalazIntegration {

  implicit def functionK[F[_], G[_]](implicit fK: scalaz.~>[F, G]): useless.algebras.FunctionK[F, G] =
    new useless.algebras.FunctionK[F, G] {

      def apply[A](fa: F[A]): G[A] = fK[A](fa)
    }

  implicit def monadError[F[_], E](implicit me: scalaz.MonadError[F, E]): useless.algebras.MonadError[F, E] =
    new useless.algebras.MonadError[F, E] {

      def pure[A](value:       A): F[A] = me.pure(value)
      def flatMap[A, B](fa:    F[A])(f: A => F[B]): F[B] = me.bind(fa)(f)
      def raiseError[A](error: E): F[A] = me.raiseError(error)
      def recoverWith[A](fa:   F[A])(f: PartialFunction[E, F[A]]): F[A] =
        me.handleError(fa) { e =>
          if (f.isDefinedAt(e)) f(e) else raiseError(e)
        }
    }

  implicit def sequence[F[_]: scalaz.Applicative]: useless.algebras.Sequence[F] =
    new useless.algebras.Sequence[F] {

      def sequence[A](lfa: List[F[A]]): F[List[A]] = scalaz.Traverse[List].sequence(lfa)
    }

  implicit val taskTimer: useless.algebras.Timer[scalaz.ioeffect.Task] =
    (duration: FiniteDuration) => scalaz.ioeffect.Task.sleep(duration)
}
