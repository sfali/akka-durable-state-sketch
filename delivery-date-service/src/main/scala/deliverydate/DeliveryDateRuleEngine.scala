package deliverydate

import cats.data.ValidatedNel
import cats.implicits._
import deliverydate.DeliveryDateEntity.DeliveryDateState

import java.time.Instant
import java.time.temporal.ChronoUnit

// TODO doesnt update old event, treats everything as new
object DeliveryDateRuleEngine {
  def evaluate(
    newEventId: Int,
    state: DeliveryDateState
  ): ValidatedNel[String, Instant] = {
    (state.deliveryDate, state.recentEventId) match {
      case (Some(currentDeliveryDate), Some(currentEventId)) =>
        if (currentEventId == 2525 || currentEventId == 3535) {
          // No further updates allowed after consuming 2525/3535 event. Use current date.
          currentDeliveryDate.validNel
        } else {
          process(newEventId)
        }
      case (None, None) =>
        process(newEventId)
      case _ => "Unable to calculate DeliveryDate".invalidNel
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
