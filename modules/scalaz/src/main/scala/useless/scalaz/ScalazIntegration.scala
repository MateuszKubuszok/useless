package useless.scalaz

import scalaz.std.list._

trait ScalazIntegration {

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
}