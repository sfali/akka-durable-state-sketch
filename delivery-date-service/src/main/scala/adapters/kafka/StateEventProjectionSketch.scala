package adapters.kafka

import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.persistence.query.typed.{EventEnvelope => QueryEventEnvelope}
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import deliverydate.DeliveryDateEntity.Event
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.projection.{Projection, ProjectionId, eventsourced}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import akka.stream.scaladsl.Source
//import akka.projection.kafka.scaladsl.KafkaProducer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.AtLeastOnceFlowProjection
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{ AtLeastOnceProjection, SourceProvider }
import akka.projection.{ ProjectionBehavior, ProjectionId }

class StateEventProjectionSketch(implicit val system: ActorSystem[_]) {

  private val numberOfSliceRanges: Int = 4
  private val sliceRanges =
    EventSourcedProvider.sliceRanges(
      system,
      JdbcReadJournal.Identifier,
      numberOfSliceRanges
    )

  private val minSlice: Int = sliceRanges.head.min
  private val maxSlice: Int = sliceRanges.head.max
  // TODO is this right?
  private val entityType: String = "DeliveryDateState"


  val kafkaTopic = "delivery-date-events"

  val kafkaProducerSettings: ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")


  private val sourceProvider: SourceProvider[Offset, QueryEventEnvelope[Event]] = {
    EventSourcedProvider.eventsBySlices[Event](
      system,
      JdbcReadJournal.Identifier,
      entityType,
      minSlice,
      maxSlice
    )
  }

  def createProducer: SendProducer[String, Array[Byte]] = ???


  // TODO finish building handler
  // TODO finish building sendProducer
  // TODO bring in ScalikeJdbcSession (Wtf is that even)


  JdbcProjection.exactlyOnce(
    projectionId = ProjectionId("DeliveryDateProjection", "DeliveryDate"),
    sourceProvider,
    handler = () => ???,
    sessionFactory = () => ???,
  )






}
