package useless.example

import java.util.UUID

import com.typesafe.scalalogging.Logger
import io.circe.generic.auto._
import useless._
import useless.algebras._
import useless.algebras.syntax._
import useless.circe._

import scala.concurrent.duration._

class AdminServices[F[_]: Manager: MonadError[?[_], Throwable]](userServices: UserServices[F],
                                                                entitlementServices: EntitlementServices[F]) {

  private val maxAttempts         = 5
  private val initialDelay        = 2.seconds
  private val delayIncreaseFactor = 2

  private val createAdminService = Manager[F].apply("create-admin-v1") {
    val logger = Logger("useless.example.create-admin-v1")
    logger.info("registering service")
    ProcessBuilder
      .create[F, (User, UUID)]
      .retryWithBounds[(UUID, UUID)](
        {
          case (user, resourceID) =>
            logger.info(s"Attempt to create user: $user")
            userServices.fetchUser(user.id).flatMap(user2 => (user2.id -> resourceID).pure[F]).recoverWith[Throwable] {
              case _: Throwable =>
                userServices.createUser(user).flatMap { _ =>
                  logger.info(s"User created successfully: $user")
                  (user.id -> resourceID).pure[F]
                }
            }
        }
      )(
        {
          case (userID, resourceID) =>
            logger.info(s"Reverting user creation - deleting user $userID")
            userServices.fetchUser(userID).flatMap { user =>
              userServices.deleteUser(userID).flatMap { _ =>
                logger.info(s"Reverting user creation - user deleted successfully: $userID")
                (user, resourceID).pure[F]
              }
            }
        }
      )(maxAttempts, initialDelay, delayIncreaseFactor)
      .revertOnFirstFailureNonRevertible {
        case (userID, resourceID) =>
          logger.info(s"Creating entitlement $userID -> $resourceID with write access")
          entitlementServices.createEntitlement(Entitlement(userID, resourceID, Entitlement.Level.Write))
      }
  }

  def createAdminForResource(user: User, resourceID: UUID): F[Unit] = createAdminService((user, resourceID))
}
