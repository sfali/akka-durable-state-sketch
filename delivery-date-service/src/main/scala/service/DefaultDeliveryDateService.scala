package service

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.util.Timeout
import deliverydate.DeliveryDateEntity
import deliverydate.DeliveryDateEntity.{DeliveryDate, GetDeliveryDate, Reply, UpdateDeliveryDate, UpdateFailed, UpdateSuccessful}
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DefaultDeliveryDateService(
  clusterSharding: ClusterSharding
)(implicit ec: ExecutionContext,
  implicit val system: ActorSystem[_])
    extends DeliveryDateService {

  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def upsertDeliveryDate(
    packageId: UUID,
    updatedDate: Instant
  ): Future[String] = {
    clusterSharding
      .entityRefFor(DeliveryDateEntity.TypeKey, packageId.toString)
      .ask[Reply] { ref =>
        UpdateDeliveryDate(packageId, updatedDate, ref)
      }
      .map {
        case UpdateSuccessful(packageId) => s"Update for packageId: $packageId was successful."
        case UpdateFailed(packageId, reason) => s"Update failed for: $packageId due to: $reason"
        case _ => s"Unexpected reply from DeliveryDateService."
      }
  }

  override def getDeliveryDate(packageId: UUID): Future[Option[Instant]] = {
    clusterSharding
      .entityRefFor(DeliveryDateEntity.TypeKey, packageId.toString)
      .ask[DeliveryDate] { ref =>
        GetDeliveryDate(packageId, ref)
      }
      .map { reply =>
        reply.deliveryDate
      }
  }
}
