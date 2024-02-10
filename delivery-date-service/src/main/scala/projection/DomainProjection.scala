package projection

import akka.actor.typed.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.query.typed.EventEnvelope
import akka.persistence.query.{DurableStateChange, Offset}
import akka.persistence.r2dbc.query.scaladsl.R2dbcReadJournal
import akka.persistence.r2dbc.state.scaladsl.R2dbcDurableStateStore
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.r2dbc.scaladsl.R2dbcProjection
import akka.projection.scaladsl.SourceProvider
import akka.projection.state.scaladsl.DurableStateSourceProvider
import akka.projection.{Projection, ProjectionId}
import deliverydate.DeliveryDateEntity
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration.DurationInt

object DomainProjection {

  def projection(
    sliceRange: Range
  )(implicit
    system: ActorSystem[_]
  ): Projection[EventEnvelope[DeliveryDateEntity.Event]] = {

    val topic = system
      .settings
      .config
      .getString("delivery-date-service.projections.topic")

    val minSlice = sliceRange.min
    val maxSlice = sliceRange.max
    val projectionId = ProjectionId("DomainEvents", s"domain-$minSlice-$maxSlice")

    def sourceProvider(
      sliceRange: Range
    ): SourceProvider[Offset, EventEnvelope[DeliveryDateEntity.Event]] =
      EventSourcedProvider.eventsBySlices[DeliveryDateEntity.Event](
        system,
        readJournalPluginId = R2dbcReadJournal.Identifier,
        DeliveryDateEntity.EntityName,
        sliceRange.min,
        sliceRange.max
      )

    R2dbcProjection.atLeastOnceAsync(
      projectionId = projectionId,
      settings = None,
      sourceProvider(sliceRange),
      handler = () =>
        new DomainEventsProjectionHandler(
          system,
          topic,
          sendProducer(s"$minSlice-$maxSlice")
        )
    )
  }

  def changesBySlices(
    sliceRange: Range
  )(implicit
    system: ActorSystem[_]
  ): Projection[DurableStateChange[DeliveryDateEntity.DeliveryDateState]] = {
    val topic = system
      .settings
      .config
      .getString("delivery-date-service.projections.topic")

    val sourceProvider =
      DurableStateSourceProvider.changesBySlices[DeliveryDateEntity.DeliveryDateState](
        system = system,
        durableStateStoreQueryPluginId = R2dbcDurableStateStore.Identifier,
        entityType = DeliveryDateEntity.EntityName,
        minSlice = sliceRange.min,
        maxSlice = sliceRange.max
      )

    val projectionKey = s"change-${sliceRange.min}-${sliceRange.max}"
    R2dbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("EgressProjection", projectionKey),
      settings = None,
      sourceProvider = sourceProvider,
      handler = () =>
        new EgressProjectionHandler(
          system = system,
          topic = topic,
          sendProducer = sendProducer(projectionKey)
        )
    )
  }

  private def sendProducer(
    suffix: String
  )(implicit
    system: ActorSystem[_]
  ): SendProducer[String, String] = {
    val producerSettings =
      ProducerSettings(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers("localhost:9092")
        .withClientId(s"egress-projection-$suffix")
        .withCloseTimeout(0.seconds)

    SendProducer(producerSettings)
  }
}
