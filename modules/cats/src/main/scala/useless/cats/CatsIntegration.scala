package useless.cats

import cats.instances.list._

trait CatsIntegration {

  implicit def monadError[F[_], E](implicit me: cats.MonadError[F, E]): useless.algebras.MonadError[F, E] =
    new useless.algebras.MonadError[F, E] {

      def pure[A](value:       A): F[A] = me.pure(value)
      def flatMap[A, B](fa:    F[A])(f: A => F[B]): F[B] = me.flatMap(fa)(f)
      def raiseError[A](error: E): F[A] = me.raiseError(error)
      def recoverWith[A](fa:   F[A])(f: PartialFunction[E, F[A]]): F[A] = me.recoverWith(fa)(f)
    }

  implicit def sequence[F[_]: cats.Applicative]: useless.algebras.Sequence[F] =
    new useless.algebras.Sequence[F] {

      def sequence[A](lfa: List[F[A]]): F[List[A]] = cats.Traverse[List].sequence(lfa)
    }
}
