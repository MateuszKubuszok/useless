package useless.internal

import useless.{ Journal, MonadError }

final case class ServiceContext[F[_]](serviceName:             String,
                                      journal:                 Journal[F],
                                      implicit val monadError: MonadError[F, Throwable])
