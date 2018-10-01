package useless.internal

//import java.util.UUID

import useless._

import scala.annotation.tailrec

private[useless] sealed trait Run[F[_], I, O] {

  def apply(context: ServiceContext[F]): ManagedService[F, I, O] = new ManagedService[F, I, O](this)(context)
}

private[useless] object Run {

  final case class RunStage[F[_], I, M, O](current: Stage[F, I, M], next: Run[F, M, O]) extends Run[F, I, O]
  final case class Stop[F[_], A]() extends Run[F, A, A]

  def fromProcess[F[_], I, O](process: Process[F, I, O]): Run[F, I, O] = {
    @tailrec
    def helper[M1](currentProcess: Process[F, I, M1], run: Run[F, M1, O]): Run[F, I, O] =
      currentProcess match {
        case _: Process.Init[F, I]           => run
        case p: Process.Proceed[F, I, _, M1] => helper(p.current, Run.RunStage(p.next, run))
      }
    helper(process, Run.Stop())
  }
}
