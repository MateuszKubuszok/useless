package useless

package object algebras {

  type MonadThrowable[F[_]] = MonadError[F, Throwable]
}
