package service

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.util.Timeout
import deliverydate.DeliveryDate
import deliverydate.DeliveryDate.{Reply, UpdateDeliveryDate}
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

  val TypeKey: EntityTypeKey[DeliveryDate.Command] =
    EntityTypeKey[DeliveryDate.Command]("DeliveryDate")

  override def upsertDeliveryDate(
    packageId: UUID,
    updatedDate: Instant
  ): Future[String] = {
    clusterSharding
      .entityRefFor(TypeKey, packageId.toString)
      .ask[Reply] { ref =>
        UpdateDeliveryDate(packageId, updatedDate, ref)
      }.map { reply =>
        log.info(s"Got reply back from DeliveryDate Entity: $reply")
        reply.toString
      }
  }
}
