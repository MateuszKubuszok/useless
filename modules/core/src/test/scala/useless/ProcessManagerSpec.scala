package useless

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import useless.algebras.MonadThrowable
import useless.algebras.syntax.AllSyntax

abstract class ProcessManagerSpec extends Specification with AllSyntax {

  abstract class WithManager[F[_]](implicit val monadThrowable: MonadThrowable[F]) extends Scope {
    val manager = createManager
  }

  protected def createJournal[F[_]: MonadThrowable]: Journal[F] = InMemoryJournal[F]
  protected def createManager[F[_]: MonadThrowable]: Manager[F] = Manager[F](createJournal)

  protected def err(msg: String): Throwable = new Exception(msg)

  protected def failThenPass[F[_]: MonadThrowable, I, O](error: Throwable)(f: I => F[O]): I => F[O] = {
    var alreadyFailed = false
    i =>
      if (alreadyFailed) f(i) else { alreadyFailed = true; error.raiseError[F, O] }
  }
}
