package deliverydate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{ChangeEventHandler, DurableStateBehavior, Effect, ReplyEffect}
import cats.data.Validated.{Invalid, Valid}

import java.time.Instant
import java.util.UUID
import scala.annotation.unused
import scala.concurrent.duration.DurationInt

object DeliveryDateEntity {

  val EntityName = "DeliveryDate"
  val TypeKey: EntityTypeKey[DeliveryDateEntity.Command] =
    EntityTypeKey[DeliveryDateEntity.Command](EntityName)

  final case class DeliveryDateState(
    packageId: UUID,
    recentEventId: Int,
    deliveryDate: Option[Instant],
    previousDeliveryDate: Option[Instant],
    updated: Instant,
    eventLog: List[String]) {

    def isDeliveryDateUpdated: Boolean =
      previousDeliveryDate match {
        case Some(date) => !deliveryDate.contains(date)
        case None       => deliveryDate.isDefined
      }
  }

  sealed trait Event {
    val packageId: UUID
  }

  final case class DeliveryDateUpdated(id: UUID, event: String) extends Event {
    override val packageId: UUID = id
  }

  private final case class DeliveryDateEvent(id: UUID, event: String) extends Event {
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
  final case class DeliveryDate(packageId: UUID, deliveryDate: Option[Instant]) extends Reply

  @unused
  private val stateChangeEventHandler =
    ChangeEventHandler[Command, DeliveryDateState, Event](
      updateHandler = { case (oldState, newState, UpdateDeliveryDate(packageId, eventId, _)) =>
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

  private val commandHandler: (Option[DeliveryDateState], Command) => ReplyEffect[Option[DeliveryDateState]] = {
    (maybeState, command) =>
      command match {
        case UpdateDeliveryDate(packageId, eventId, replyTo) =>
          val state = maybeState.getOrElse(
            DeliveryDateState(
              packageId = packageId,
              recentEventId = 0,
              deliveryDate = None,
              previousDeliveryDate = None,
              updated = Instant.EPOCH,
              eventLog = List.empty
            )
          )

          DeliveryDateRuleEngine.evaluate(eventId, state) match {
            case Valid(validatedDate) =>
              val processTime = Instant.now()
              val eventDescription =
                s"eventId: $eventId processedAt: $processTime updated date: $validatedDate"

              Effect
                .persist(
                  Some(
                    DeliveryDateState(
                      packageId = packageId,
                      recentEventId = eventId,
                      deliveryDate = Some(validatedDate),
                      previousDeliveryDate = state.deliveryDate,
                      updated = processTime,
                      eventLog = state.eventLog :+ eventDescription
                    )
                  )
                )
                .thenReply(replyTo)(_ => UpdateSuccessful(packageId))

            case Invalid(e) =>
              Effect
                .none
                .thenReply(replyTo)(_ => UpdateFailed(packageId, reason = e.toString()))

          }
      }
  }

  def apply(
    packageId: UUID
  ): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      context.log.info("Initializing: {}", packageId)
      DurableStateBehavior
        .withEnforcedReplies[Command, Option[DeliveryDateState]](
          persistenceId = PersistenceId(TypeKey.name, packageId.toString),
          emptyState = None,
          commandHandler = commandHandler
        )
        .onPersistFailure(
          SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
        )
    }
}
