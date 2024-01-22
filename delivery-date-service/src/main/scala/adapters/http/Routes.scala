package adapters.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import service.DeliveryDateService

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Routes(deliveryDateService: DeliveryDateService) {

  private final case class MockedExternalEvent(eventId: Int)

  private def processEvent(
    packageId: UUID,
    eventId: Int
  ): Future[String] = {
    deliveryDateService.updateDeliveryDate(packageId, eventId)
  }

  private def retrieveDeliveryDate(packageId: UUID): Future[String] = {
    deliveryDateService.getDeliveryDate(packageId).map {
      case Some(date) => s"PackageId: $packageId has delivery date: $date"
      case None => s"PackageId: $packageId has not had an initial delivery date"
    }
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
            onSuccess(retrieveDeliveryDate(UUID.fromString(packageId))) {
              response =>
                complete(s"$response")
            }
          }
      }
    }
  }
}
