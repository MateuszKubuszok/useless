package useless.internal

import useless.Journal
import useless.algebras.MonadError

final case class ServiceContext[F[_]](serviceName:             String,
                                      journal:                 Journal[F],
                                      implicit val monadError: MonadError[F, Throwable])
