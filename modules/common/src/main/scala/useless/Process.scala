package useless

sealed trait Process[F[_], I, O] {

  def addStage[O2](stage: Stage[F, O, O2]): Process[F, I, O2] = Process.Proceed[F, I, O, O2](this, stage)
}

object Process {

  def init[F[_], A]: Process[F, A, A] = Init()

  private[useless] final case class Init[F[_], A]() extends Process[F, A, A]
  private[useless] final case class Proceed[F[_], I, M, O](current: Process[F, I, M], next: Stage[F, M, O])
      extends Process[F, I, O]
}
