package org.mashupbots.socko.examples.websocket

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.{PoisonPill, ActorSystem, Props, actorRef2Scala}
import akka.dispatch.OnComplete

/**
 * This example shows how to use web sockets, specifically `org.mashupbots.socko.processors.WebSocketBroadcaster`,
 * for chatting.
 *
 * With `org.mashupbots.socko.processors.WebSocketBroadcaster`, you can broadcast messages to all registered web
 * socket connections
 *
 * - Open a few browsers and navigate to `http://localhost:8888/html`.
 * - A HTML page will be displayed
 * - It will make a web socket connection to `ws://localhost:8888/websocket/`
 * - Type in some text on one browser and see it come up on the other browsers
 */
object ChatApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // `ChatHandler` is created in the route and is self-terminating
  //
  val actorSystem = ActorSystem("ChatExampleActorSystem")

  //
  // STEP #2 - Define Routes
  // Each route dispatches the request to a newly instanced `WebSocketHandler` actor for processing.
  // `WebSocketHandler` will `stop()` itself after processing the request.
  //
  val routes = Routes({

    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[ChatHandler]) ! httpRequest
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") => {
        // To start Web Socket processing, we first have to authorize the handshake.
        // This is a security measure to make sure that web sockets can only be established at your specified end points.

        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))

        val webSocketId = wsHandshake.webSocketId
        actorSystem.actorOf(Props(new FooHandler(webSocketId)), name="fooHandler") ! Push("foo")

      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      //actorSystem.actorOf(Props[ChatHandler]) ! wsFrame
      actorSystem.actorSelection("user/fooHandler") ! wsFrame
    }

  })

  val webServer = new WebServer(WebServerConfig(), routes, actorSystem)

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open a few browsers and navigate to http://localhost:8888/html. Start chatting!")
  }

  def onWebSocketHandshakeComplete(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId connected")
  }

  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
    actorSystem.actorSelection("user/fooHandler") ! PoisonPill
  }

}