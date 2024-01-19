package deliverydate

import akka.actor.typed.ActorRef
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{ DurableStateBehavior, Effect }
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID

object DeliveryDateEntity {

  private val log = LoggerFactory.getLogger(this.getClass)

  final case class DeliveryDateState(
    packageId: UUID,
    deliveryDate: Option[Instant],
    updated: Instant)

  sealed trait Command
  final case class UpdateDeliveryDate(
    packageId: UUID,
    updatedDate: Instant,
    replyTo: ActorRef[UpdateSuccessful])
      extends Command

  final case class GetDeliveryDate(
    packageId: UUID,
    replyTo: ActorRef[DeliveryDate])
      extends Command

  trait Reply
  final case class UpdateSuccessful(packageId: UUID) extends Reply
  final case class DeliveryDate(packageId: UUID, deliveryDate: Option[Instant])
      extends Reply

  def apply(
    packageId: UUID
  ): DurableStateBehavior[Command, DeliveryDateState] = {
    DurableStateBehavior[Command, DeliveryDateState](
      persistenceId = PersistenceId.ofUniqueId(packageId.toString),
      emptyState = DeliveryDateState(
        packageId = packageId,
        deliveryDate = None,
        updated = Instant.now()
      ),
      commandHandler = (state, command) =>
        command match {
          case UpdateDeliveryDate(packageId, updatedDate, replyTo) =>
            log.info(
              s"PackageId: $packageId from: ${state.deliveryDate} to $updatedDate"
            )
            replyTo ! UpdateSuccessful(packageId)
            Effect.persist(
              DeliveryDateState(packageId, Some(updatedDate), Instant.now())
            )

          case GetDeliveryDate(packageId, replyTo) =>
            replyTo ! DeliveryDate(packageId, state.deliveryDate)
            Effect.none
        }
    )
  }
}
