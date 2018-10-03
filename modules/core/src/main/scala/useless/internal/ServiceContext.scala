package useless.internal

import useless.Journal
import useless.algebras.{ MonadError, Timer }

final case class ServiceContext[F[_]](serviceName:             String,
                                      journal:                 Journal[F],
                                      logger:                  String => Unit,
                                      implicit val monadError: MonadError[F, Throwable],
                                      implicit val timer:      Timer[F])
