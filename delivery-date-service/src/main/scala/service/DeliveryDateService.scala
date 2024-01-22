package service

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

trait DeliveryDateService {
  def updateDeliveryDate(packageId: UUID, eventId: Int): Future[String]
  def getDeliveryDate(packageId: UUID): Future[Option[Instant]]
}
