package useless.internal

//import java.util.UUID

import useless._

private[useless] sealed trait RunAST[F[_], I, O] {

  def apply(context: ServiceContext[F]): ManagedService[F, I, O] = new ManagedService[F, I, O](this)(context)
}

private[useless] object RunAST {

  final case class Proceed[F[_], I, M, O](current: Stage[F, I, M], next: RunAST[F, M, O]) extends RunAST[F, I, O]
  final case class Stop[F[_], A]() extends RunAST[F, A, A]

  def fromProcessBuilder[F[_], I, O](process: ProcessBuilder[F, I, O]): RunAST[F, I, O] = {
    // 2.11 apparently cannot optimize it out into tailrec
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def helper[M1](currentProcess: ProcessBuilder[F, I, M1], run: RunAST[F, M1, O]): RunAST[F, I, O] =
      currentProcess match {
        case _: ProcessBuilder.Init[F, I]           => run
        case p: ProcessBuilder.Proceed[F, I, _, M1] => helper(p.current, RunAST.Proceed(p.next, run))
      }
    helper(process, RunAST.Stop())
  }
}
