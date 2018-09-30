package object useless {

  implicit def toStageOps[F[_], A, B](f: A => F[B]): StageOps[F, A, B] = new StageOps(f)
}
