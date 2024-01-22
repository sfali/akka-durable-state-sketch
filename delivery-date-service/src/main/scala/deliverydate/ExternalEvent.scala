package deliverydate

import java.time.Instant

// TODO, the ExternalEvent wont contain the deliveryDate. Its this systems job to calculate it.
// Replace with processedTime or something
case class ExternalEvent (packageId: String, eventId: Int, deliveryDate: Instant)
