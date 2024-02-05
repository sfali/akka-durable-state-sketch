package adapters.kafka

import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl._
import deliverydate.ExternalEvent
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.kafka.common.serialization._
import org.slf4j.LoggerFactory
import service.DeliveryDateService

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object DeliveryDateServiceKafkaAdapter {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val groupId = "delivery-date-ingress"
  private val topic = "external-events"

  def consumeEventsFromKafka(
    deliveryDateService: DeliveryDateService,
  )(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers("localhost:9092")
        .withGroupId(groupId)
        .withStopTimeout(0.seconds)

    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map(record => {
        decode[ExternalEvent](record.value())
      })
      .collect { case Right(event) => event }
      .mapAsync(4) { event =>
        deliveryDateService
          .updateDeliveryDate(
            UUID.fromString(event.packageId),
            event.eventId
          )
          .recover { case ex: Throwable =>
            s"DeliveryDateService failed to process event due to ${ex.getMessage}"
          }
      }
      .map { result =>
        log.info(s"*** Result back from DeliveryDateService: $result")
        result
      }
      .runWith(Sink.ignore)
  }
}
