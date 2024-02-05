import adapters.http.{DeliveryDateHttpServer, Routes}
import adapters.kafka.DeliveryDateServiceKafkaAdapter
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import deliverydate.DeliveryDateEntity
import deliverydate.DeliveryDateEntity.{Command, TypeKey}
import projection.DomainEventsProjectionRunner
import query.DeliveryDateStateRepository
import service.DefaultDeliveryDateService

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object DeliveryDateApp {

  def main(args: Array[String]): Unit = {
    ActorSystem[Command](behavior(), "delivery-date-app")
  }

  private def behavior(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      implicit val system: ActorSystem[_] = context.system

      val clusterSharding = ClusterSharding(system)

      clusterSharding.init(Entity(TypeKey) { entityContext =>
        DeliveryDateEntity(UUID.fromString(entityContext.entityId))
      })

      val deliveryDateService = new DefaultDeliveryDateService(clusterSharding)
      val repo = new DeliveryDateStateRepository

      DeliveryDateHttpServer.start(
        routes = new Routes(repo).routes,
        port = 1234
      )

      DeliveryDateServiceKafkaAdapter.consumeEventsFromKafka(deliveryDateService)

      DomainEventsProjectionRunner.run

      Behaviors.empty
    }
  }
}
