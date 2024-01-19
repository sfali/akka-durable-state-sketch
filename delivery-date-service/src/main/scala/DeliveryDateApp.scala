import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import deliverydate.DeliveryDate
import deliverydate.DeliveryDate.Command
import http.{DeliveryDateHttpServer, Routes}
import service.DefaultDeliveryDateService

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object DeliveryDateApp {

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("DeliveryDate")

  def main(args: Array[String]): Unit = {
    ActorSystem[Command](behavior(), "delivery-date-app")
  }

  private def behavior(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      implicit val system: ActorSystem[_] = context.system

      val clusterSharding = ClusterSharding(system)

      clusterSharding.init(Entity(TypeKey) { entityContext =>
        DeliveryDate(UUID.fromString(entityContext.entityId))
      })

      val deliveryDateService = new DefaultDeliveryDateService(clusterSharding)

      DeliveryDateHttpServer.start(
        routes = new Routes(deliveryDateService).routes,
        port = 1234,
        system
      )

      Behaviors.empty
    }
  }
}
