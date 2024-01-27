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

  // Events need the packageId as it will be used to key the kafka projection
  sealed trait Event {
    val packageId: UUID
  }
  // TODO Replace once functioning better understood
  final case class SomethingHappened(id: UUID, event: String) extends Event {
    override val packageId: UUID = id
  }

  sealed trait Command
  final case class UpdateDeliveryDate(
    packageId: UUID,
    eventId: Int,
    replyTo: ActorRef[Reply])
      extends Command

  final case class GetDeliveryDateState(
    packageId: UUID,
    replyTo: ActorRef[DeliveryDateState])
      extends Command

  trait Reply
  final case class UpdateSuccessful(packageId: UUID) extends Reply
  final case class UpdateFailed(packageId: UUID, reason: String) extends Reply
  final case class DeliveryDate(packageId: UUID, deliveryDate: Option[Instant])
      extends Reply

  // TODO:
  // This is typed to a very specific command, is that allowed?
  // Having a GET as a request kind of breaks this api, I dont want to emit and event into the journal on that
  // How does this know where to write it to?
  private val stateChangeEventHandler =
    ChangeEventHandler[Command, DeliveryDateState, Event](
      updateHandler = {
        case (_, _, UpdateDeliveryDate(packageId, eventId, _)) =>
          SomethingHappened(packageId, s"$eventId was processed.")
        case (_, _, GetDeliveryDateState(packageId, _)) =>
          SomethingHappened(
            packageId,
            s"get request was made."
          ) // I dont want this
      },
      deleteHandler = { (state, _) =>
        SomethingHappened(state.packageId, "I dont know what this is")
      }
    )

  def apply(
    packageId: UUID
  ): DurableStateBehavior[Command, DeliveryDateState] = {
    DurableStateBehavior[Command, DeliveryDateState](
      persistenceId = PersistenceId.ofUniqueId(packageId.toString),
      emptyState = DeliveryDateState(
        packageId = packageId,
        recentEventId = None,
        deliveryDate = None,
        updated = Instant.now(),
        eventLog = List.empty
      ),
      commandHandler = (state, command) =>
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

          case GetDeliveryDateState(_, replyTo) =>
            replyTo ! state
            Effect.none
        }
    )
      .withChangeEventHandler(stateChangeEventHandler)
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )
  }
}
