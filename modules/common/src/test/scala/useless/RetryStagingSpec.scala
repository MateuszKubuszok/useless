package useless

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification

import scala.concurrent.Future

class RetryStagingSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  "happy path should return correct result" in {
    // given
    val journal = new InMemoryJournal[Future]
    val manager = new Manager[Future](journal)

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
}
