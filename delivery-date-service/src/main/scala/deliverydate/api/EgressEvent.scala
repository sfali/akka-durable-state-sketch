package deliverydate.api

import java.time.Instant
import java.util.UUID

sealed trait EgressEvent extends Product with Serializable {
  val id: UUID
}
final case class DeliveryDateUpdated(id: UUID, event: Int, date: Instant) extends EgressEvent
final case class DeliveryDateNotUpdated(id: UUID, event: Int, date: Instant) extends EgressEvent
