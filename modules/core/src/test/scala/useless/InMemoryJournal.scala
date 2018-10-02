package useless

import java.util.UUID

import useless.Journal.RawServiceState
import useless.algebras.{ MonadError, MonadThrowable }

class InMemoryJournal[F[_]](implicit monadError: MonadError[F, Throwable]) extends Journal[F] {

  import monadError._
  import scala.collection.mutable

  private val storage: mutable.Map[String, mutable.Map[UUID, RawServiceState]] = mutable.Map.empty

  def persistRawStatus(state: RawServiceState): F[Unit] =
    map(pure(state)) { s =>
      storage.getOrElseUpdate(s.serviceName, mutable.Map.empty).update(s.callID, s)
    }

  def fetchRawStates(serviceName: String): F[List[RawServiceState]] =
    map(pure(serviceName)) { n =>
      storage.get(n).toList.flatMap(_.values.toList)
    }

  def removeRawStates(callIDs: List[UUID]): F[Unit] =
    map(pure(callIDs)) { cs =>
      storage.values.foreach { map =>
        cs.foreach(map.remove)
      }
    }
}

object InMemoryJournal {

  def apply[F[_]: MonadThrowable]: InMemoryJournal[F] = new InMemoryJournal[F]
}
