package useless

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  def main(args: Array[String]): Unit = {

    val journal: Journal[Future] = Journal.inMemory[Future]
    val manager = Manager[Future](journal)

    val forward:  Int => Future[String] = i => Future.successful(i.toString)
    val backward: String => Future[Int] = s => Future.successful(s.toInt)

    val managedService = manager("test") {
      ProcessBuilder.create[Future, Int].retryUntilSucceed(forward)(backward)
    }

    managedService(1).foreach(println)
  }
}
