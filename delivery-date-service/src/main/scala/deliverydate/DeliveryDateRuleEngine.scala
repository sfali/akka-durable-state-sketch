package deliverydate

import cats.data.ValidatedNel
import cats.implicits._
import deliverydate.DeliveryDateEntity.DeliveryDateState

import java.time.Instant
import java.time.temporal.ChronoUnit

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
          validateEventId(newEventId, Some(currentDeliveryDate))
        }

      case (None, None) =>
        validateEventId(newEventId, None)
      case _ => "Unable to calculate DeliveryDate".invalidNel
    }
  }

  /*
    - EventId must be 4 digits in length, otherwise considered invalid.

    - Based on business rules if a package has received it's first ever event scan then the Delivery Date is 30 days
    from now.  Any further scans between [0, 1000] bring the date closer by 5 days, scans between [1001, 5000]
    bring it closer by 10. If the date is in the past then reject it.
   */
  private def validateEventId(
    newEventId: Int,
    currentDeliveryDate: Option[Instant]
  ): ValidatedNel[String, Instant] = {

    newEventId.toString match {
      case id if id.length == 4 =>
        currentDeliveryDate match {
          case Some(date) =>
            val updatedDate =
              if (0 <= newEventId && newEventId <= 1000) {
                date.minus(5, ChronoUnit.DAYS)
              } else if (newEventId > 1000 && newEventId <= 5000) {
                date.minus(10, ChronoUnit.DAYS)
              } else {
                date
              }

            // Ensure that new updatedDate DeliveryDate is sooner than previous one, but also exists in the future
            if (updatedDate.isBefore(date) && updatedDate.isAfter(Instant.now())) {
              updatedDate.validNel
            } else {
              "Invalid DeliveryDate generated".invalidNel
            }
          case None => Instant.now().plus(30, ChronoUnit.DAYS).validNel
        }
      case _ => "Invalid EventId".invalidNel
    }
  }
}
