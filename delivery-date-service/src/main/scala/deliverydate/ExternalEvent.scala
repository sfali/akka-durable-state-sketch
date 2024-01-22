package deliverydate

import java.time.Instant

case class ExternalEvent (packageId: String, eventId: Int, deliveryDate: Instant)
