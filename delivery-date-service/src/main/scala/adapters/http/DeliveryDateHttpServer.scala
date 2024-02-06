package adapters.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object DeliveryDateHttpServer {

  private val log = LoggerFactory.getLogger(this.getClass)

  def start(routes: Route, port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    Http()
      .newServerAt("localhost", port)
      .bind(routes)
      .onComplete {
        case Success(binding) =>
          log
            .info(
              s"DeliveryDateHttpServer HttpServer online at ${binding.localAddress.getHostString}:${binding.localAddress.getPort}"
            )
        case Failure(exception) =>
          log.error(s"Failed to bind HTTP server $exception")
          system.terminate()
      }
  }
}
