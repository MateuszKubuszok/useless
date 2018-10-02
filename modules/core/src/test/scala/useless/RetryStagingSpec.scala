package useless

import java.util.UUID

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import useless.Journal.StageStatus

import scala.concurrent.Future

class RetryStagingSpec(implicit ee: ExecutionEnv) extends ProcessManagerSpec with FutureMatchers {

  type F[A] = Future[A]

  "stage in process with retry until succeed strategy" should {

    "return correct result for a happy path" in new WithManager[F] {
      // given
      val i2fs: Int => Future[String] = _.toString.pure[F]
      val s2fi: String => Future[Int] = _.toInt.pure[F]

      val addSuffix:    String => Future[String] = s => s"$s test".pure[F]
      val removeSuffix: String => Future[String] = _.replaceFirst(" test", "").pure[F]

      val managedService = manager("test") {
        ProcessBuilder.create[Future, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = managedService(10)

      // then
      result must be_==("10 test").await
    }

    "retry after failure" in new WithManager[F] {
      // given
      val i2fs: Int => Future[String] = failThenPass(err("first stage fail"))(_.toString.pure[F])
      val s2fi: String => Future[Int] = _.toInt.pure[F]

      val addSuffix:    String => Future[String] = failThenPass(err("second stage fail"))(s => s"$s test".pure[F])
      val removeSuffix: String => Future[String] = _.replaceFirst(" test", "").pure[F]

      val managedService = manager("test") {
        ProcessBuilder.create[Future, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = managedService(10)

      // then
      result must be_==("10 test").await
    }

    "retry after JVM crash" in new WithManager[F] {
      // given
      val i2fs: Int => Future[String] = failThenPass(err("first stage fail"))(_.toString.pure[F])
      val s2fi: String => Future[Int] = _.toInt.pure[F]

      val addSuffix:    String => Future[String] = failThenPass(err("second stage fail"))(s => s"$s test".pure[F])
      val removeSuffix: String => Future[String] = _.replaceFirst(" test", "").pure[F]

      journal.persistState[Int](Journal.ServiceState[Int]("test", UUID.randomUUID, 1, 10, StageStatus.Started))
      journal.persistState[Int](Journal.ServiceState[Int]("test", UUID.randomUUID, 1, 20, StageStatus.Finished))

      val managedService = manager("test") {
        ProcessBuilder.create[Future, Int].retryUntilSucceed(i2fs)(s2fi).retryUntilSucceed(addSuffix)(removeSuffix)
      }

      // when
      val result = manager.retryServicesInDB().map(_.toSet)

      // then
      result must be_==(Set(Right("10 test"), Right("20"))).await
    }
  }
}
