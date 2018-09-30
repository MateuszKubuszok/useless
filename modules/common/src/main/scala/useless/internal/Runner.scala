package useless.internal

//import java.util.UUID

import useless._

import scala.annotation.tailrec

private[useless] sealed trait Runner[F[_], I, O] {

  def apply(serviceContext: ServiceContext[F]): ManagedService[F, I, O] = {
    println("test")
    println(serviceContext)
    // TODO: build ManagedService from a runner
    //
    ???
  }

//  def getStage(no: Int): Unit = {}
//
//  def runStage[M](serviceContext: ServiceContext[F], no: Int, execution: UUID): I => F[O] = {
////    import serviceContext.journalist._
//    implicit val me: MonadError[F, Throwable] = serviceContext.monadError
//    this match {
//      case Runner.RunStage(current, _) => ???
//      case Runner.Stop() => me.pure[O]
//    }
//  }
}

private[useless] object Runner {

  final case class RunStage[F[_], I, M, O](current: Stage[F, I, M], next: Runner[F, M, O]) extends Runner[F, I, O]
  final case class Stop[F[_], A]() extends Runner[F, A, A]

  def fromProcess[F[_], I, O](process: Process[F, I, O]): Runner[F, I, O] = {
    @tailrec
    def helper[M1](currentProcess: Process[F, I, M1], runner: Runner[F, M1, O]): Runner[F, I, O] =
      currentProcess match {
        case _: Process.Init[F, I]           => runner
        case p: Process.Proceed[F, I, _, M1] => helper(p.current, Runner.RunStage(p.next, runner))
      }
    helper(process, Runner.Stop())
  }
}
