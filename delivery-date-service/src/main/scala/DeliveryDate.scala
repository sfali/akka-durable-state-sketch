import akka.actor.typed.ActorRef
import akka.persistence.typed.state.scaladsl.{ DurableStateBehavior, Effect }
import akka.persistence.typed.PersistenceId

import java.time.Instant
import java.util.UUID

object DeliveryDate {

  final case class DeliveryDateState(
    packageId: UUID,
    deliveryDate: Option[Instant],
    updated: Instant)

  sealed trait Command
  final case class UpdateDeliveryDate(
    packageId: UUID,
    updatedDate: Instant,
    replyTo: ActorRef[Reply])
      extends Command

  sealed trait Reply
  final case class UpdateSuccessful(packageId: UUID) extends Reply

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

            println(state.packageId + " date: " + state.deliveryDate)

            val updatedState =
              DeliveryDateState(packageId, Some(updatedDate), Instant.now())
            replyTo ! UpdateSuccessful(packageId)
            Effect.persist(updatedState)
        }
    )
  }
}
