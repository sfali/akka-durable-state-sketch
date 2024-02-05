package query

import scala.concurrent.Future

trait DeliveryDateRepository {
  def getDeliveryDateInformation(packageId: String): Future[String]
}
