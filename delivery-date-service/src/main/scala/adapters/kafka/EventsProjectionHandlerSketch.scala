package adapters.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import deliverydate.DeliveryDateEntity.Event

import scala.concurrent.Future

class EventsProjectionHandlerSketch(
  system: ActorSystem[_],
  topic: String,
  sendProducer: SendProducer[String, Array[Byte]])
    extends Handler[EventEnvelope[Event]] {
  override def process(envelope: EventEnvelope[Event]): Future[Done] = ???
}
