package useless

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  import syntax._

  def main(args: Array[String]): Unit = {

    val journal: Journal[Future] = Journal.inMemory[Future]
    val manager = Manager[Future](journal)

    val testService: Int => Future[String] = i => Future.successful(i.toString)

    val managedService = manager("test") {
      ProcessBuilder.create[Future, Int].addStage(testService.retryUntilSucceed)
    }

    managedService(1).foreach(println)
  }
}
