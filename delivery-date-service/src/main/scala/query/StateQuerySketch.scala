package query

import akka.actor.typed.ActorSystem
import akka.persistence.state.DurableStateStoreRegistry
import akka.persistence.query.scaladsl.DurableStateStoreQuery
import akka.persistence.query.{DeletedDurableState, DurableStateChange, Offset, UpdatedDurableState}
import deliverydate.DeliveryDateEntity.DeliveryDateState
import akka.persistence.state.DurableStateStoreRegistry
import akka.persistence.r2dbc.state.scaladsl.R2dbcDurableStateStore

import scala.concurrent.ExecutionContext


object StateQuerySketch {

  // TODO refactor - redesign
  // needs persistence Id, probably just return state as string?
  // TODO add plugin string
  def do_something(implicit system: ActorSystem[_]) {
    implicit val ec: ExecutionContext = system.executionContext

    val durableStateStoreQuery: R2dbcDurableStateStore[DeliveryDateState] = DurableStateStoreRegistry(system)
      .durableStateStoreFor[R2dbcDurableStateStore[DeliveryDateState]](
        R2dbcDurableStateStore.Identifier // Is this right?
      )

    durableStateStoreQuery.getObject("").map { result =>
      result.value.map { state =>
        state.packageId
      }

    }
  }

}
