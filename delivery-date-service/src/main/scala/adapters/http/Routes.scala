package adapters.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import service.DeliveryDateService

import java.util.UUID
import scala.concurrent.Future

class Routes(deliveryDateService: DeliveryDateService) {

  // TODO remove PUT route
  private final case class MockedExternalEvent(eventId: Int)

  private def processEvent(
    packageId: UUID,
    eventId: Int
  ): Future[String] = {
    deliveryDateService.updateDeliveryDate(packageId, eventId)
  }

  private def retrieveDeliveryDateState(packageId: UUID): Future[String] = {
    deliveryDateService.getDeliveryDateState(packageId)
  }

  val routes: Route = {
    pathPrefix("packageId") {
      path(Segment) { packageId =>
        put {
          entity(as[MockedExternalEvent]) { request =>
            onSuccess(
              processEvent(UUID.fromString(packageId), request.eventId)
            ) { response =>
              complete(s"$response")
            }
          }
        } ~
          get {
            onSuccess(retrieveDeliveryDateState(UUID.fromString(packageId))) {
              response =>
                complete(s"$response")
            }
          }
      }
    }
  }
}
