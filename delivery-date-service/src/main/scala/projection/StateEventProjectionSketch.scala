package projection

import akka.actor.typed.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.jdbc.state.scaladsl.JdbcDurableStateStore
//import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{DurableStateChange, Offset}
import akka.projection.ProjectionId
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.SourceProvider
import akka.projection.state.scaladsl.DurableStateSourceProvider
import deliverydate.DeliveryDateEntity
import org.apache.kafka.common.serialization.StringSerializer
import slick.jdbc.JdbcBackend.Database

object StateEventProjectionSketch {

  private val numberOfSliceRanges: Int = 4
  private val entityType: String = "DeliveryDateState"

  val topic = "delivery-date-events"

  def startProjectionToKafka(
    database: Database
  )(implicit system: ActorSystem[_]
  ): Unit = {


    val sourceProvider
      : SourceProvider[Offset, DurableStateChange[DeliveryDateEntity.Event]] = {
      // This appears to be reading the journal and looking back how far the events go
      // and creating a slice range to process? so min/max is all?
      val sliceRanges: Seq[Range] =
        DurableStateSourceProvider.sliceRanges(
          system,
          durableStateStoreQueryPluginId = JdbcDurableStateStore.Identifier,
          numberOfSliceRanges
        )

      DurableStateSourceProvider.changesBySlices[DeliveryDateEntity.Event](
        system,
        durableStateStoreQueryPluginId = JdbcDurableStateStore.Identifier,
        entityType,
        sliceRanges.head.min,
        sliceRanges.head.max
      )
    }

    val sendProducer: SendProducer[String, String] = {
      val producerSettings =
        ProducerSettings(system, new StringSerializer, new StringSerializer)
          .withBootstrapServers("localhost:9092")

      SendProducer(producerSettings)
    }

    // What is the ID for this?  How does it connect to the DeliveryDate entity?
    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("DeliveryDateProjection", "DeliveryDate"),
      sourceProvider,
      handler =
        () => new EventsProjectionHandlerSketch(system, topic, sendProducer),
      sessionFactory = () => new SlickDbSession(database)
    )
  }
}
