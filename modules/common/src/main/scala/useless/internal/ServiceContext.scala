package useless.internal

import useless.{ Journalist, MonadError }

final case class ServiceContext[F[_]](serviceName: String,
                                      journalist:  Journalist[F],
                                      monadError:  MonadError[F, Throwable])
