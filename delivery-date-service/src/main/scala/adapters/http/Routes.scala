package adapters.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import query.DeliveryDateRepository

import scala.concurrent.Future

class Routes(repository: DeliveryDateRepository) {

  private def retrieveDeliveryDateState(packageId: String): Future[String] = {
    repository.getDeliveryDateInformation(packageId)
  }

  val routes: Route = {
    pathPrefix("packageId") {
      path(Segment) { packageId =>
         get {
            onSuccess(retrieveDeliveryDateState(packageId)) {
              response =>
                complete(s"Service replied with: $response")
            }
          }
      }
    }
  }
}
