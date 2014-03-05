import akka.actor.{Props, ActorSystem}
import java.io.{BufferedInputStream, DataInputStream, BufferedOutputStream, DataOutputStream}
import java.net.Socket
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.mashupbots.socko.routes._

object SockoApp extends Logger {

  val actorSystem = ActorSystem("SockoExampleActorSystem")

  val host = "localhost"
  val port = 4567
  //
  // STEP #2 - Define Routes
  // Each route dispatches the request to a newly instanced `TimeHandler` actor for processing.
  // `TimeHandler` will `stop()` itself after processing each request.
  //
  val routes = Routes({

    case HttpRequest(request) => request match {

      case GET(Path("/foobar")) => {
        actorSystem.actorOf(Props[FoobarHandler]) ! FoobarRequest(request, host, port)
      }
    }

  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to: ")
    System.out.println(" http://localhost:8888/foobar")
  }

}
