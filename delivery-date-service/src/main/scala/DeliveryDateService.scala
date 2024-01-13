import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}

import java.util.UUID

object DeliveryDateService {

  private val TypeKey: EntityTypeKey[DeliveryDate.Command] =
    EntityTypeKey[DeliveryDate.Command]("DeliveryDate")

  def main(args: Array[String]): Unit = {
    Behaviors.setup[Nothing] { context =>
      val clusterSharding = ClusterSharding(context.system)

      clusterSharding.init(Entity(TypeKey) { entityContext =>
        DeliveryDate(UUID.fromString(entityContext.entityId))
      })

      Behaviors.empty
    }
  }
}
