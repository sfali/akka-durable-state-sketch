import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.util.Timeout
import deliverydate.DeliveryDate

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object DeliveryDateService {

  private val TypeKey: EntityTypeKey[DeliveryDate.Command] =
    EntityTypeKey[DeliveryDate.Command]("DeliveryDate")

  // TODO replace this
  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[DeliveryDate.Command](behavior(), "delivery-date-service")

    val replyHandler = system.systemActorOf(ReplyHandler(), "replyHandler")

    Thread.sleep(3000)

    val packageId = "0a0ae225-d116-49e4-84ef-fb39cc2fe612"

    val clusterSharding = ClusterSharding(system)
    val entityRef = clusterSharding.entityRefFor(TypeKey, packageId)

    val command = DeliveryDate.UpdateDeliveryDate(UUID.fromString(packageId), Instant.now().minusSeconds(10000), replyHandler)

    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = system.scheduler
    implicit val ec = system.executionContext

    val responseFuture =
      entityRef.ask[DeliveryDate.Reply](ref => command.copy(replyTo = ref))

    responseFuture.onComplete {
      case Success(response) => println(s"Received response: $response")
      case Failure(exception) =>
        println(s"Failed to receive response: $exception")
    }

    val command1 = DeliveryDate.UpdateDeliveryDate(UUID.fromString(packageId), Instant.now().minusSeconds(10000), replyHandler)

    val responseFuture1 =
      entityRef.ask[DeliveryDate.Reply](ref => command1.copy(replyTo = ref))

    responseFuture.onComplete {
      case Success(response) => println(s"Received response: $response")
      case Failure(exception) =>
        println(s"Failed to receive response: $exception")
    }

  }

  private def behavior(): Behavior[DeliveryDate.Command] = {
    Behaviors.setup[DeliveryDate.Command] { context =>
      implicit val system: ActorSystem[_] = context.system

      val clusterSharding = ClusterSharding(system)

      clusterSharding.init(Entity(TypeKey) { entityContext =>
        DeliveryDate(UUID.fromString(entityContext.entityId))
      })

      Behaviors.empty
    }
  }
}
object ReplyHandler {
  def apply(): Behavior[DeliveryDate.Reply] = Behaviors.receiveMessage {
    case DeliveryDate.UpdateSuccessful(packageId) =>
      println(s"Update confirmed for packageId $packageId")
      Behaviors.same
  }
}
