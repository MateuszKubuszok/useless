package useless.syntax
import useless.MonadError

class MonadErrorOps[F[_], A](val fa: F[A]) extends AnyVal {

  def flatMap[B](f: A => F[B])(implicit monadError: MonadError[F, Throwable]): F[B] = monadError.flatMap(fa)(f)

  def map[B](f: A => B)(implicit monadError: MonadError[F, Throwable]): F[B] = monadError.map(fa)(f)

  def foreach[B](f: A => B)(implicit monadError: MonadError[F, Throwable]): Unit = monadError.foreach(fa)(f)

  def recover(f: PartialFunction[Throwable, A])(implicit monadError: MonadError[F, Throwable]): F[A] =
    monadError.recover(fa)(f)

  def recoverWith(f: PartialFunction[Throwable, F[A]])(implicit monadError: MonadError[F, Throwable]): F[A] =
    monadError.recoverWith(fa)(f)
}
