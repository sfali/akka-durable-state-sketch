package projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.typed.EventEnvelope
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }

class DomainEventsProjectionHandler(
  system: ActorSystem[_],
  topic: String,
  sendProducer: SendProducer[String, String])
    extends Handler[EventEnvelope[DeliveryDateEntity.Event]] {
  private implicit val ec: ExecutionContext =
    system.executionContext

  private val log = LoggerFactory.getLogger(this.getClass)

  override def process(
    envelope: EventEnvelope[DeliveryDateEntity.Event]
  ): Future[Done] = {

    envelope.event match {
      case DeliveryDateEntity.DeliveryDateUpdated(id, event) =>
        val producerRecord = new ProducerRecord(topic, id.toString, event)

        sendProducer.send(producerRecord).map { metadata =>
          log.info(s"Published event to topic - metadata [$metadata]")
          Done
        }
      case _ => Future.successful(Done)
    }
  }
}
