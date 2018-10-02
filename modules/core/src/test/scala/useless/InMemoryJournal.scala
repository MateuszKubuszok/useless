package useless

import java.util.UUID

import useless.Journal.RawServiceState
import useless.algebras.MonadThrowable
import useless.algebras.syntax._

class InMemoryJournal[F[_]: MonadThrowable] extends Journal[F] {

  import scala.collection.mutable

  private val storage: mutable.Map[String, mutable.Map[UUID, RawServiceState]] = mutable.Map.empty

  def persistRawState(state: RawServiceState): F[Unit] =
    state.pure[F].map { s =>
      storage.getOrElseUpdate(s.serviceName, mutable.Map.empty).update(s.callID, s)
    }

  def fetchRawStates(serviceName: String): F[List[RawServiceState]] =
    serviceName.pure[F].map { n =>
      storage.get(n).toList.flatMap(_.values.toList)
    }

  def removeRawStates(callIDs: List[UUID]): F[Unit] =
    callIDs.pure[F].map { cs =>
      storage.values.foreach { map =>
        cs.foreach(map.remove)
      }
    }
}

object InMemoryJournal {

  def apply[F[_]: MonadThrowable]: InMemoryJournal[F] = new InMemoryJournal[F]
}
