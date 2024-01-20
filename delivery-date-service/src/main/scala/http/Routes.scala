package http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import service.DeliveryDateService

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class Routes(deliveryDateService: DeliveryDateService) {

  private final case class UpdatedDate(updatedDate: Instant)

  private def processEvent(
    packageId: UUID,
    updatedDate: Instant
  ): Future[String] = {
    deliveryDateService.upsertDeliveryDate(packageId, updatedDate)
  }

  private def retrieveDeliveryDate(packageId: UUID): Future[String] = {
    deliveryDateService.getDeliveryDate(packageId).map {
      case Some(date) => s"PackageId: $packageId has delivery date: $date"
      case None => s"PackageId: $packageId has not had an initial delivery date"
    }
  }

  val routes: Route = {
    pathPrefix("package") {
      path(Segment) { packageId =>
        put {
          entity(as[UpdatedDate]) { request =>
            onSuccess(
              processEvent(UUID.fromString(packageId), request.updatedDate)
            ) { response =>
              complete(s"Result: $response")
            }
          }
        } ~
          get {
            onSuccess(retrieveDeliveryDate(UUID.fromString(packageId))) {
              response =>
                complete(s"Result: $response")
            }
          }
      }
    }
  }
}
