package http

import akka.http.scaladsl.server.Route
import service.DeliveryDateService
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import java.time.Instant
import java.util.UUID
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import scala.concurrent.Future

class Routes(deliveryDateService: DeliveryDateService) {

  final case class UpdatedDate(updatedDate: Instant)

  private def processEvent(
    packageId: UUID,
    updatedDate: Instant
  ): Future[String] = {
    deliveryDateService.upsertDeliveryDate(packageId, updatedDate)
  }

  val routes: Route = {
    pathPrefix("device") {
      path(Segment) { deviceId =>
        put {
          entity(as[UpdatedDate]) { request =>
            onSuccess(
              processEvent(UUID.fromString(deviceId), request.updatedDate)
            ) { response =>
              complete(s"Result: $response")
            }
          }
        }
      }
    }
  }
}
