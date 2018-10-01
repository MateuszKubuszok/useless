package useless

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

object Main {

  def main(args: Array[String]): Unit = {

    val journal: Journal[Future] = Journal.inMemory[Future]
    val manager = Manager[Future](journal)

    val forward:  Int => Future[String] = i => Future.successful(i.toString)
    val backward: String => Future[Int] = s => Future.successful(s.toInt)

    val managedService = manager("test") {
      ProcessBuilder.create[Future, Int].retryUntilSucceed(forward)(backward)
    }

    Await.result(managedService(1).transform(t => Try(println(t))), Duration.Inf)
  }
}
