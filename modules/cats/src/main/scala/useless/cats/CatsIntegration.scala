package useless.cats

import cats.instances.list._

import scala.concurrent.duration.FiniteDuration

trait CatsIntegration {

  implicit def makeFunctionKCats[F[_], G[_]](
    implicit fK: cats.arrow.FunctionK[F, G]
  ): useless.algebras.FunctionK[F, G] =
    new useless.algebras.FunctionK[F, G] {

      def apply[A](fa: F[A]): G[A] = fK[A](fa)
    }

  implicit def makeMonadErrorCats[F[_], E](implicit me: cats.MonadError[F, E]): useless.algebras.MonadError[F, E] =
    new useless.algebras.MonadError[F, E] {

      def pure[A](value:       A): F[A] = me.pure(value)
      def flatMap[A, B](fa:    F[A])(f: A => F[B]): F[B] = me.flatMap(fa)(f)
      def raiseError[A](error: E): F[A] = me.raiseError(error)
      def recoverWith[A](fa:   F[A])(f: PartialFunction[E, F[A]]): F[A] = me.recoverWith(fa)(f)
    }

  implicit def makeSequenceCats[F[_]: cats.Applicative]: useless.algebras.Sequence[F] =
    new useless.algebras.Sequence[F] {

      def sequence[A](lfa: List[F[A]]): F[List[A]] = cats.Traverse[List].sequence(lfa)
    }

  implicit def makeTimerCats[F[_]: cats.effect.Timer]: useless.algebras.Timer[F] =
    (duration: FiniteDuration) => implicitly[cats.effect.Timer[F]].sleep(duration)
}
