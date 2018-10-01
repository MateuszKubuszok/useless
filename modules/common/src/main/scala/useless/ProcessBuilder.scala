package useless

sealed trait ProcessBuilder[F[_], I, O] {

  def addStage[O2](stage: Stage[F, O, O2]): ProcessBuilder[F, I, O2] = ProcessBuilder.Proceed[F, I, O, O2](this, stage)
}

object ProcessBuilder {

  def create[F[_], A]: ProcessBuilder[F, A, A] = Init()

  private[useless] final case class Init[F[_], A]() extends ProcessBuilder[F, A, A]
  private[useless] final case class Proceed[F[_], I, M, O](current: ProcessBuilder[F, I, M], next: Stage[F, M, O])
      extends ProcessBuilder[F, I, O]
}
