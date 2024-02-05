package service

import java.util.UUID
import scala.concurrent.Future

trait DeliveryDateService {
  def updateDeliveryDate(packageId: UUID, eventId: Int): Future[String]
}
