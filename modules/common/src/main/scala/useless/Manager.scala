package useless

import useless.internal.{ Run, ServiceContext }

class Manager[F[_]](journalist: Journalist[F])(implicit monadError: MonadError[F, Throwable]) {

  // TODO: on each service creation run unfinished services in journal

  def apply[A, B](serviceName: String)(process: Process[F, A, B]): A => F[B] =
    Run.fromProcess(process)(ServiceContext(serviceName, journalist, monadError))
}

object Manager {

  def apply[F[_]](journalist: Journalist[F])(implicit monadError: MonadError[F, Throwable]): Manager[F] =
    new Manager[F](journalist)
}
