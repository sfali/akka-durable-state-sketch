package query

import akka.actor.typed.ActorSystem
import akka.persistence.r2dbc.state.scaladsl.R2dbcDurableStateStore
import akka.persistence.state.DurableStateStoreRegistry
import akka.persistence.typed.PersistenceId
import deliverydate.DeliveryDateEntity.{ DeliveryDateState, TypeKey }

import scala.concurrent.{ ExecutionContext, Future }

class DeliveryDateStateRepository(implicit system: ActorSystem[_])
    extends DeliveryDateRepository {

  private val durableStateStoreQuery
    : R2dbcDurableStateStore[DeliveryDateState] =
    DurableStateStoreRegistry(system)
      .durableStateStoreFor[R2dbcDurableStateStore[DeliveryDateState]](
        R2dbcDurableStateStore.Identifier
      )

  def getDeliveryDateInformation(packageId: String): Future[String] = {
    implicit val ec: ExecutionContext = system.executionContext

    val persistenceId = PersistenceId(TypeKey.name, packageId)

    durableStateStoreQuery
      .getObject(persistenceId.id)
      .map { result =>
        {
          result
            .value
            .map { state =>
              val response =
                s"""
                   | packageId: ${state.packageId}
                   | eventLog:  ${state.eventLog}
                   | deliveryDate: ${state.deliveryDate}
                   | eventId: ${state.recentEventId}
                   |""".stripMargin

              response
            }
            .getOrElse(s"No results found for $packageId")
        }
      }
  }
}
