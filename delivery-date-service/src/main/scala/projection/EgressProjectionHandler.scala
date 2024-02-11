package projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.{DeletedDurableState, DurableStateChange, UpdatedDurableState}
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity
import deliverydate.DeliveryDateEntity.DeliveryDateState
import deliverydate.api.{DeliveryDateNotUpdated, DeliveryDateUpdated, EgressEvent}
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.Future

class EgressProjectionHandler(
  system: ActorSystem[_],
  topic: String,
  sendProducer: SendProducer[String, String])
    extends Handler[DurableStateChange[DeliveryDateEntity.DeliveryDateState]] {

  import system.executionContext
  private val log = LoggerFactory.getLogger(this.getClass)

  override def process(envelope: DurableStateChange[DeliveryDateEntity.DeliveryDateState]): Future[Done] =
    envelope match {
      case update: UpdatedDurableState[_] =>
        val event = toEgressEvent(update.value)
        val producerRecord = new ProducerRecord(topic, event.id.toString, event.asJson.noSpaces)
        sendProducer.send(producerRecord).map { metadata =>
          log.info(s"Published event to topic - metadata [$metadata]")
          Done
        }

      case _: DeletedDurableState[_] =>
        log.warn("Delete is not supported yet")
        Future.successful(Done)
    }

  private def toEgressEvent(state: DeliveryDateState): EgressEvent = {
    log.info("Consuming: {}", state)
    if (state.isDeliveryDateUpdated) {
      DeliveryDateUpdated(
        id = state.packageId,
        event = state.recentEventId.get,
        date = state.deliveryDate.get
      )
    } else {
      DeliveryDateNotUpdated(
        id = state.packageId,
        event = state.recentEventId.get,
        date = state.deliveryDate.get
      )
    }
  }

}
