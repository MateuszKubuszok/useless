package useless.algebras

trait FunctionK[F[_], G[_]] {

  def apply[A](fa: F[A]): G[A]
}
