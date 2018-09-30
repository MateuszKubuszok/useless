package useless.internal

trait ManagedService[F[_], A, B] extends (A => F[B])
