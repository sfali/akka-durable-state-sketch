package deliverydate

import cats.data.ValidatedNel
import cats.implicits._

import java.time.Instant
import java.time.temporal.ChronoUnit

object DeliveryDateRuleEngine {
  def evaluate(
    newEventId: Int,
    maybeCurrentEventId: Option[Int],
    maybeCurrentDeliveryDate: Option[Instant]
  ): ValidatedNel[String, Instant] = {
    (maybeCurrentDeliveryDate, maybeCurrentEventId) match {
      case (Some(currentDeliveryDate), Some(currentEventId)) =>
        if (currentEventId == 2525 || currentEventId == 3535) {
          // No further updates allowed after consuming 2525/3535 event. Use current date.
          currentDeliveryDate.validNel
        } else {
          process(newEventId)
        }
      case (None, None) =>
        process(newEventId)
    }
  }

  private def process(newEventId: Int): ValidatedNel[String, Instant] = {
    if (0 <= newEventId && newEventId <= 1000) {
      Instant.now.plus(5, ChronoUnit.DAYS).validNel
    } else if (newEventId > 1000 && newEventId <= 5000) {
      Instant.now.plus(10, ChronoUnit.DAYS).validNel
    } else {
      "EventId outside valid range [0, 5000]".invalidNel
    }
  }
}
