package adapters.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity.Event
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class EventsProjectionHandlerSketch(
  system: ActorSystem[_],
  topic: String,
  sendProducer: SendProducer[String, String])
    extends Handler[EventEnvelope[Event]] {
  private implicit val ec: ExecutionContext =
    system.executionContext

  private val log = LoggerFactory.getLogger(getClass)

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {

    val event = envelope.event

    // TODO What should the key be? How is that determined?
    val producerRecord = new ProducerRecord(topic, "", s"Dummy Event: $event")

    sendProducer.send(producerRecord).map { metadata =>
      log.info(s"Published event to topic - metadata [$metadata")
      Done
    }
  }
}
