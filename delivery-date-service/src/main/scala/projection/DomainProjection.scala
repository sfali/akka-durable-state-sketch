package projection

import akka.actor.typed.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.Offset
import akka.persistence.query.typed.EventEnvelope
import akka.persistence.r2dbc.query.scaladsl.R2dbcReadJournal
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.r2dbc.scaladsl.R2dbcProjection
import akka.projection.scaladsl.SourceProvider
import akka.projection.{Projection, ProjectionId}
import deliverydate.DeliveryDateEntity
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration.DurationInt

object DomainProjection {

  def projection(
    sliceRange: Range
  )(implicit system: ActorSystem[_]
  ): Projection[EventEnvelope[DeliveryDateEntity.Event]] = {

    val topic = system
      .settings
      .config
      .getString("delivery-date-service.projections.topic")
    val entityType: String = "DeliveryDate"

    val minSlice = sliceRange.min
    val maxSlice = sliceRange.max
    val projectionId =
      ProjectionId("DomainEvents", s"domain-$minSlice-$maxSlice")

    def sourceProvider(
      sliceRange: Range
    ): SourceProvider[Offset, EventEnvelope[DeliveryDateEntity.Event]] = {
      EventSourcedProvider.eventsBySlices[DeliveryDateEntity.Event](
        system,
        readJournalPluginId = R2dbcReadJournal.Identifier,
        entityType,
        sliceRange.min,
        sliceRange.max
      )
    }

    def sendProducer(
      minSlice: Int,
      maxSlice: Int
    ): SendProducer[String, String] = {
      val producerSettings =
        ProducerSettings(system, new StringSerializer, new StringSerializer)
          .withBootstrapServers("localhost:9092")
          .withClientId(s"egress-projection-$minSlice-$maxSlice")
          .withCloseTimeout(0.seconds)

      SendProducer(producerSettings)
    }

    R2dbcProjection.atLeastOnceAsync(
      projectionId = projectionId,
      settings = None,
      sourceProvider(sliceRange),
      handler = () =>
        new DomainEventsProjectionHandler(
          system,
          topic,
          sendProducer(minSlice, maxSlice)
        )
    )
  }
}
