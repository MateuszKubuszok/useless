package useless

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification

import scala.concurrent.Future

class RetryStagingSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  "happy path should return correct result" in {
    // given
    val journal = InMemoryJournal[Future]
    val manager = Manager[Future](journal)

    val i2fs: Int => Future[String] = i => Future.successful(i.toString)
    val s2fi: String => Future[Int] = s => Future.successful(s.toInt)

    val addSuffix:    String => Future[String] = s => Future.successful(s + " test")
    val removeSuffix: String => Future[String] = s => Future.successful(s.replaceFirst(" test", ""))

    val managedService = manager("test") {
      ProcessBuilder.create[Future, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
    }

    // when
    val result = managedService(10)

    // then
    result must be_==("10 test").await
  }

  "should retry after failure" in {
    // given
    val journal = InMemoryJournal[Future]
    val manager = Manager[Future](journal, (s: String) => println(s))

    val i2fs: Int => Future[String] =
      failThenPass(new Exception("first stage fail"))(i => Future.successful(i.toString))
    val s2fi: String => Future[Int] = s => Future.successful(s.toInt)

    val addSuffix: String => Future[String] =
      failThenPass(new Exception("second stage fail"))(s => Future.successful(s + " test"))
    val removeSuffix: String => Future[String] = s => Future.successful(s.replaceFirst(" test", ""))

    val managedService = manager("test") {
      ProcessBuilder.create[Future, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
    }

    // when
    val result = managedService(10)

    // then
    result must be_==("10 test").await
  }

  private def failThenPass[I, O](error: Throwable)(f: I => Future[O]): I => Future[O] = {
    var alreadyFailed = false
    i =>
      if (alreadyFailed) f(i) else { alreadyFailed = true; Future.failed(error) }
  }
}
