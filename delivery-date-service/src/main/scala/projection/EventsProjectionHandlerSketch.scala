package projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.{DeletedDurableState, DurableStateChange, UpdatedDurableState}
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class EventsProjectionHandlerSketch(
  system: ActorSystem[_],
  topic: String,
  sendProducer: SendProducer[String, String])
    extends Handler[DurableStateChange[DeliveryDateEntity.Event]] {
  private implicit val ec: ExecutionContext =
    system.executionContext

  private val log = LoggerFactory.getLogger(this.getClass)

  override def process(
    envelope: DurableStateChange[DeliveryDateEntity.Event]
  ): Future[Done] = {

    val (id, event) = envelope match {
      case state: UpdatedDurableState[DeliveryDateEntity.Event] =>
        state.value match {
          case DeliveryDateEntity.SomethingHappened(id, event) => (id.toString, s"Dummy Event: $event - $id")
        }
      case state: DeletedDurableState[DeliveryDateEntity.Event] => (state.persistenceId, s"Dummy Event: ${state.persistenceId}")
    }

    val producerRecord = new ProducerRecord(topic, id, event)

    sendProducer.send(producerRecord).map { metadata =>
      log.info(s"Published event to topic - metadata [$metadata]")
      Done
    }
  }
}
