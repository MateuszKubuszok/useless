package useless

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  import syntax._

  def main(args: Array[String]): Unit = {

    val journalist: Journalist[Future] = Journalist.inMemory[Future]
    val manager = Manager[Future](journalist)

    val testService: Int => Future[String] = i => Future.successful(i.toString)

    val managedService = manager("test") {
      Process.init[Future, Int].addStage(testService.retryUntilSucceed)
    }

    managedService(1).foreach(println)
  }
}
