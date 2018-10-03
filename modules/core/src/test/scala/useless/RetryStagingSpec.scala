package useless

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import useless.Journal.StageStatus

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class RetryStagingSpec(implicit ee: ExecutionEnv) extends ProcessManagerSpec with FutureMatchers {

  type F[A] = Future[A]

  "stage in process with retry until succeed strategy" should {

    "return correct result for a happy path" in new WithManager[F] {
      // given
      val i2fs: Int => F[String] = _.toString.pure[F]
      val s2fi: String => F[Int] = _.toInt.pure[F]

      val addSuffix:    String => F[String] = s => s"$s test".pure[F]
      val removeSuffix: String => F[String] = _.replaceFirst(" test", "").pure[F]

      val managedService = manager("test") {
        ProcessBuilder.create[F, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = managedService(10)

      // then
      result must be_==("10 test").await
    }

    "retry after failure" in new WithManager[F] {
      // given
      val i2fs: Int => F[String] = failThenPass(err("first stage fail"))(_.toString.pure[F])
      val s2fi: String => F[Int] = _.toInt.pure[F]

      val addSuffix:    String => F[String] = failThenPass(err("second stage fail"))(s => s"$s test".pure[F])
      val removeSuffix: String => F[String] = _.replaceFirst(" test", "").pure[F]

      val managedService = manager("test") {
        ProcessBuilder.create[F, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = managedService(10)

      // then
      result must be_==("10 test").await
    }

    "resume after JVM crash" in new WithManager[F] {
      // given
      val i2fs: Int => F[String] = failThenPass(err("first stage fail"))(_.toString.pure[F])
      val s2fi: String => F[Int] = _.toInt.pure[F]

      val addSuffix:    String => F[String] = failThenPass(err("second stage fail"))(s => s"$s test".pure[F])
      val removeSuffix: String => F[String] = _.replaceFirst(" test", "").pure[F]

      Await.result(stubUnfinishedCall("test", 1, 10, StageStatus.Started), 1.second)
      Await.result(stubUnfinishedCall("test", 1, "20", StageStatus.Finished), 1.second)

      val managedService = manager("test") {
        ProcessBuilder.create[F, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = manager.resumeInterruptedServicesUnsafe().map(_.toSet)

      // then
      result must be_==(Set(Right("10 test"), Right("20 test"))).await
    }
  }
}
