package projection

import akka.actor.typed.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.persistence.query.typed.{EventEnvelope => QueryEventEnvelope}
import akka.projection.ProjectionId
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.SourceProvider
import deliverydate.DeliveryDateEntity.Event
import org.apache.kafka.common.serialization.StringSerializer
import slick.jdbc.JdbcBackend.Database

// TODO This should be elsewhere its not an adapter
object StateEventProjectionSketch {

  def startProjectionToKafka(database: Database)(implicit system: ActorSystem[_]): Unit = {

    val numberOfSliceRanges: Int = 4

    val sliceRanges =
      EventSourcedProvider.sliceRanges(
        system,
        JdbcReadJournal.Identifier,
        numberOfSliceRanges
      )

    val minSlice: Int = sliceRanges.head.min
    val maxSlice: Int = sliceRanges.head.max
    // TODO is this right?
    val entityType: String = "DeliveryDateState"

    val topic = "delivery-date-events"

    val sourceProvider
    : SourceProvider[Offset, QueryEventEnvelope[Event]] = {
      EventSourcedProvider.eventsBySlices[Event](
        system,
        JdbcReadJournal.Identifier,
        entityType,
        minSlice,
        maxSlice
      )
    }

    val sendProducer: SendProducer[String, String] = {
      val producerSettings: ProducerSettings[String, String] =
        ProducerSettings(system, new StringSerializer, new StringSerializer)
          .withBootstrapServers("localhost:9092")

      SendProducer(producerSettings)
    }

    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("DeliveryDateProjection", "DeliveryDate"),
      sourceProvider,
      handler =
        () => new EventsProjectionHandlerSketch(system, topic, sendProducer),
      sessionFactory = () => new SlickDbSession(database)
    )
  }
}
