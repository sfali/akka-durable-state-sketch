package deliverydate

import akka.actor.typed.{ ActorRef, SupervisorStrategy }
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{
  ChangeEventHandler,
  DurableStateBehavior,
  Effect
}
import cats.data.Validated.{ Invalid, Valid }
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt

object DeliveryDateEntity {

  val TypeKey: EntityTypeKey[DeliveryDateEntity.Command] =
    EntityTypeKey[DeliveryDateEntity.Command]("DeliveryDate")

  final case class DeliveryDateState(
    packageId: UUID,
    recentEventId: Option[Int],
    deliveryDate: Option[Instant],
    updated: Instant,
    eventLog: List[String])

  sealed trait Event {
    val packageId: UUID
  }

  final case class DeliveryDateUpdated(id: UUID, event: String) extends Event {
    override val packageId: UUID = id
  }

  private final case class DeliveryDateEvent(id: UUID, event: String)
      extends Event {
    override val packageId: UUID = id
  }

  sealed trait Command
  final case class UpdateDeliveryDate(
    packageId: UUID,
    eventId: Int,
    replyTo: ActorRef[Reply])
      extends Command

  trait Reply
  final case class UpdateSuccessful(packageId: UUID) extends Reply
  final case class UpdateFailed(packageId: UUID, reason: String) extends Reply
  final case class DeliveryDate(packageId: UUID, deliveryDate: Option[Instant])
      extends Reply

  private val stateChangeEventHandler =
    ChangeEventHandler[Command, DeliveryDateState, Event](
      updateHandler = {
        case (oldState, newState, UpdateDeliveryDate(packageId, eventId, _)) =>
          DeliveryDateUpdated(
            packageId,
            s"$eventId was processed for $packageId. ${oldState.deliveryDate} to ${newState.deliveryDate}"
          )
      },
      deleteHandler = { (state, _) =>
        DeliveryDateEvent(
          state.packageId,
          s" ${state.packageId} Entity deleted."
        )
      }
    )

  private val commandHandler
    : (DeliveryDateState, Command) => Effect[DeliveryDateState] = {
    (state, command) =>
      command match {
        case UpdateDeliveryDate(packageId, eventId, replyTo) =>
          DeliveryDateRuleEngine.evaluate(eventId, state) match {
            case Valid(validatedDate) =>
              val processTime = Instant.now()
              val eventDescription =
                s"eventId: $eventId processedAt: $processTime updated date: $validatedDate"

              Effect
                .persist(
                  DeliveryDateState(
                    packageId,
                    Some(eventId),
                    Some(validatedDate),
                    processTime,
                    eventLog = state.eventLog :+ eventDescription
                  )
                )
                .thenReply(replyTo)(_ => UpdateSuccessful(packageId))
            case Invalid(e) =>
              Effect
                .none
                .thenReply(replyTo)(_ =>
                  UpdateFailed(packageId, reason = e.toString())
                )

          }
      }
  }

  def apply(
    packageId: UUID
  ): DurableStateBehavior[Command, DeliveryDateState] = {
    DurableStateBehavior[Command, DeliveryDateState](
      persistenceId = PersistenceId(TypeKey.name, packageId.toString),
      emptyState = DeliveryDateState(
        packageId = packageId,
        recentEventId = None,
        deliveryDate = None,
        updated = Instant.now(),
        eventLog = List.empty
      ),
      commandHandler = commandHandler
    )
      .withChangeEventHandler(stateChangeEventHandler)
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  }
}
