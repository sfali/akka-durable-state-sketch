import adapters.http.{DeliveryDateHttpServer, Routes}
import adapters.kafka.{DeliveryDateServiceKafkaAdapter, StateEventProjectionSketch}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import deliverydate.DeliveryDateEntity
import deliverydate.DeliveryDateEntity.{Command, TypeKey}
import service.DefaultDeliveryDateService
import slick.jdbc.JdbcBackend.Database

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

      DeliveryDateHttpServer.start(
        routes = new Routes(deliveryDateService).routes,
        port = 1234,
        system
      )

      val database = Database.forConfig("???")
      StateEventProjectionSketch.startProjectionToKafka(database)

      DeliveryDateServiceKafkaAdapter.consumeEventsFromKafka(
        deliveryDateService,
        system
      )

      Behaviors.empty
    }
  }
}
