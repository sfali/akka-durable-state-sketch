package deliverydate

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{ DurableStateBehavior, Effect }
import cats.data.Validated.{ Invalid, Valid }

import java.time.Instant
import java.util.UUID

object DeliveryDateEntity {

  val TypeKey: EntityTypeKey[DeliveryDateEntity.Command] =
    EntityTypeKey[DeliveryDateEntity.Command]("DeliveryDate")

  final case class DeliveryDateState(
    packageId: UUID,
    recentEventId: Option[Int],
    deliveryDate: Option[Instant],
    updated: Instant,
    eventLog: List[String])

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
  }
}
