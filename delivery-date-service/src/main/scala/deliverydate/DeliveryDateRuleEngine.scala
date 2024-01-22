package deliverydate

import cats.data.ValidatedNel
import cats.implicits._

import java.time.Instant
import java.time.temporal.ChronoUnit

// TODO add state so we can check previous consumed eventId
object DeliveryDateRuleEngine {
  def evaluate(
    eventId: Int,
    currentDeliveryDate: Option[Instant]
  ): ValidatedNel[String, Instant] = {
    currentDeliveryDate match {
      case Some(deliveryDate) =>
        if (eventId == 1234) {
          deliveryDate.plus(3, ChronoUnit.DAYS).validNel
        } else if (0 <= eventId && eventId <= 1000) {
          deliveryDate.plus(5, ChronoUnit.DAYS).validNel
        } else if (eventId > 1000 && eventId <= 5000) {
          deliveryDate.plus(10, ChronoUnit.DAYS).validNel
        } else {
          "EventId outside valid range [0, 5000]".invalidNel
        }
      case None => Instant.now().plus(30, ChronoUnit.DAYS).validNel
    }
  }
}
