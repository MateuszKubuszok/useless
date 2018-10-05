package useless.algebras

import java.util.concurrent.{ CountDownLatch, TimeUnit }

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ CanAwait, ExecutionContext, Future, TimeoutException }
import scala.reflect.ClassTag
import scala.util.Try

// copy-pasted from Future.never in Scala 2.12
@SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Throw"))
final object never extends Future[Nothing] {

  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    atMost match {
      case e if e eq Duration.Undefined => throw new IllegalArgumentException("cannot wait for Undefined period")
      case Duration.Inf        => new CountDownLatch(1).await()
      case Duration.MinusInf   => // Drop out
      case f: FiniteDuration   =>
        if (f > Duration.Zero) new CountDownLatch(1).await(f.toNanos, TimeUnit.NANOSECONDS)
    }
    throw new TimeoutException(s"Future timed out after [$atMost]")
  }

  @throws(classOf[Exception])
  override def result(atMost: Duration)(implicit permit: CanAwait): Nothing = {
    ready(atMost)
    throw new TimeoutException(s"Future timed out after [$atMost]")
  }

  override def onSuccess[U](pf: PartialFunction[Nothing, U])(implicit executor: ExecutionContext): Unit = ()
  override def onFailure[U](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Unit = ()
  override def onComplete[U](f: Try[Nothing] => U)(implicit executor: ExecutionContext): Unit = ()
  override def isCompleted: Boolean = false
  override def value: Option[Try[Nothing]] = None
  override def failed: Future[Throwable] = this
  override def foreach[U](f: Nothing => U)(implicit executor: ExecutionContext): Unit = ()
  override def transform[S](s: Nothing => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): Future[S] = this
  override def map[S](f: Nothing => S)(implicit executor: ExecutionContext): Future[S] = this
  override def flatMap[S](f: Nothing => Future[S])(implicit executor: ExecutionContext): Future[S] = this
  override def filter(p: Nothing => Boolean)(implicit executor: ExecutionContext): Future[Nothing] = this
  override def collect[S](pf: PartialFunction[Nothing, S])(implicit executor: ExecutionContext): Future[S] = this
  override def recover[U >: Nothing](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Future[U] = this
  override def recoverWith[U >: Nothing](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] = this
  override def zip[U](that: Future[U]): Future[(Nothing, U)] = this
  override def fallbackTo[U >: Nothing](that: Future[U]): Future[U] = this
  override def mapTo[S](implicit tag: ClassTag[S]): Future[S] = this
  override def andThen[U](pf: PartialFunction[Try[Nothing], U])(implicit executor: ExecutionContext): Future[Nothing] = this

  override def toString: String = "Future(<never>)"
}