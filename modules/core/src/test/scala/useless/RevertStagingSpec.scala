package useless

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import useless.Journal.StageStatus

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class RevertStagingSpec(implicit ee: ExecutionEnv) extends ProcessManagerSpec with FutureMatchers {

  type F[A] = Future[A]

  "stage in process with revert on first failure strategy" should {

    "return correct result for a happy path" in new WithManager[F] {
      // given
      val i2fs: Int => F[String] = _.toString.pure[F]
      val s2fi: String => F[Int] = _.toInt.pure[F]

      val addSuffix:    String => F[String] = s => s"$s test".pure[F]
      val removeSuffix: String => F[String] = _.replaceFirst(" test", "").pure[F]

      val managedService = manager("test") {
        ProcessBuilder.create[F, Int].revertOnFirstFailure(i2fs)(s2fi).revertOnFirstFailure(addSuffix)(removeSuffix)
      }

      // when
      val result = managedService(10)

      // then
      result must be_==("10 test").await
    }

    "retry after failure" in new WithManager[F] {
      // given
      var firstReverted = false
      val i2fs: Int => F[String] = _.toString.pure[F]
      val s2fi: String => F[Int] = s => { firstReverted = true; s.toInt.pure[F] }

      var secondReverted = false
      val addSuffix:    String => F[String] = s => s"$s test".pure[F]
      val removeSuffix: String => F[String] = s => { secondReverted = true; s.replaceFirst(" test", "").pure[F] }

      val failure: String => F[Unit] = _ => err("Rollback 2 stages").raiseError[F, Unit]

      val managedService = manager("test") {
        ProcessBuilder
          .create[F, Int]
          .revertOnFirstFailure(i2fs)(s2fi)
          .revertOnFirstFailure(addSuffix)(removeSuffix)
          .revertOnFirstFailureNonRevertible(failure)
      }

      // when
      val result = managedService(10)

      // then
      result must throwA(err("Rollback 2 stages")).await
      firstReverted must beTrue
      secondReverted must beTrue
    }

    "retry after JVM crash" in new WithManager[F] {
      // given
      val i2fs: Int => F[String] = _.toString.pure[F]
      val s2fi: String => F[Int] = _.toInt.pure[F]

      val addSuffix:    String => F[String] = s => s"$s test".pure[F]
      val removeSuffix: String => F[String] = _.replaceFirst(" test", "").pure[F]

      Await.result(stubUnfinishedCall("test", 1, 10, StageStatus.Started), 1.second)
      Await.result(stubUnfinishedCall("test", 1, "20", StageStatus.Finished), 1.second)

      val managedService = manager("test") {
        ProcessBuilder.create[F, Int].revertOnFirstFailure(i2fs)(s2fi).revertOnFirstFailure(addSuffix)(removeSuffix)
      }

      // when
      val result = manager.retryServicesInDB().map(_.toSet)

      // then
      result must be_==(Set(Right("10 test"), Right("20 test"))).await
    }
  }
}
