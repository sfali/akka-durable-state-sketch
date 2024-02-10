package service

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import deliverydate.DeliveryDateEntity
import deliverydate.DeliveryDateEntity._
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DefaultDeliveryDateService(
  clusterSharding: ClusterSharding
)(implicit
  val system: ActorSystem[_])
    extends DeliveryDateService {

  private val logger = LoggerFactory.getLogger(classOf[DeliveryDateService])
  import system.executionContext
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def updateDeliveryDate(
    packageId: UUID,
    eventId: Int
  ): Future[String] = {
    logger.info("About send request for: {} with eventId: {}", packageId, eventId)
    clusterSharding
      .entityRefFor(DeliveryDateEntity.TypeKey, packageId.toString)
      .ask[Reply] { ref =>
        UpdateDeliveryDate(packageId, eventId, ref)
      }
      .map {
        case UpdateSuccessful(packageId) =>
          s"Update for packageId: $packageId was successful."
        case UpdateFailed(packageId, reason) =>
          s"Update failed for: $packageId due to: $reason"
        case _ => s"Unexpected reply from DeliveryDateService."
      }
      .recover { ex =>
        s"Failed to update delivery date for packageId: $packageId due to: ${ex.getMessage}"
      }
  }
}
