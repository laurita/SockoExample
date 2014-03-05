import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.{PoisonPill, ActorSystem, Props, actorRef2Scala}

object ChatApp extends Logger {

  val actorSystem = ActorSystem("ChatExampleActorSystem")

  val routes = Routes({

    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/")) =>
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[HTTPRequestHandler]) ! httpRequest

      case Path("/favicon.ico") =>
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)

    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") =>

        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))

        val webSocketId = wsHandshake.webSocketId
        val webSocketRequestHandler = actorSystem.actorOf(Props(new WebSocketRequestHandler(webSocketId)), name="webSocketRequestHandler")
        //webSocketRequestHandler ! Push("foo")


    }

    case WebSocketFrame(wsFrame) =>

      log.info("got WebSocketFrame"+ wsFrame)

      val wsrh = actorSystem.actorSelection("user/webSocketRequestHandler")

      log.info("found WebSocketRequestHandler: "+ wsrh)
      wsrh ! wsFrame


  })

  val webServer = new WebServer(WebServerConfig(), routes, actorSystem)

  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open a few browsers and navigate to http://localhost:8888/. Start chatting!")
  }

  def onWebSocketHandshakeComplete(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
    actorSystem.actorSelection("user/fooHandler") ! PoisonPill
  }

}