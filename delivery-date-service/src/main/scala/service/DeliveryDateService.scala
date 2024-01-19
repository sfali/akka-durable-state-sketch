package service

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

trait DeliveryDateService {
  def upsertDeliveryDate(packageId: UUID, updatedDate: Instant): Future[String]
  def getDeliveryDate(packageId: UUID): Future[Option[Instant]]
}
