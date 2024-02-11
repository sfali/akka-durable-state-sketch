package projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.{DeletedDurableState, DurableStateChange, UpdatedDurableState}
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

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
        val state = update.value
        log.info("Consuming: {}", state)

        val producerRecord = new ProducerRecord(
          topic,
          state.packageId.toString,
          s"State updated: recentEventId: ${state.recentEventId.getOrElse("None")}, deliveryDate: ${state.deliveryDate.map(_.toString).getOrElse("None")}"
        )

        sendProducer.send(producerRecord).map { metadata =>
          log.info(s"Published event to topic - metadata [$metadata]")
          Done
        }

      case _: DeletedDurableState[_] =>
        log.warn("Delete is not supported yet")
        Future.successful(Done)
    }
}
