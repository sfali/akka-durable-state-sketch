package projection

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.r2dbc.query.scaladsl.R2dbcReadJournal
import akka.projection.ProjectionBehavior
import akka.projection.eventsourced.scaladsl.EventSourcedProvider

object DomainEventsProjectionRunner {

  def run(implicit system: ActorSystem[_]): Unit = {

    val numberOfSliceRanges: Int = system
      .settings
      .config
      .getInt("delivery-date-service.projections-slice-count")

    val sliceRanges = EventSourcedProvider.sliceRanges(
      system,
      R2dbcReadJournal.Identifier,
      numberOfSliceRanges
    )

    ShardedDaemonProcess(system).init(
      name = "DomainEventsProjectionRunner",
      numberOfInstances = numberOfSliceRanges,
      behaviorFactory =
        index => ProjectionBehavior(DomainProjection.projection(sliceRanges(index)))
    )
  }
}
