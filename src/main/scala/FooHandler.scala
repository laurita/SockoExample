package org.mashupbots.socko.examples.websocket

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import org.mashupbots.socko.events.WebSocketFrameEvent

import akka.actor.{ActorLogging, Actor}
import scala.concurrent._
import duration._
import ExecutionContext.Implicits._

case class Push(text: String)

/**
 * Web Socket processor for chatting
 */
class FooHandler(webSocketId: String) extends Actor with ActorLogging {

  log.info("FoobarHandler created")

  override def receive: Actor.Receive = {
    case Push(text) =>
      log.info("Push")
      pushTwice(webSocketId, text)

    /*
    case event: WebSocketFrameEvent =>
      // Echo web socket text frames
      writeWebSocketResponse(event)
      context.stop(self)
    */
  }

  /**
   * push messages every second
   */
  private def pushTwice(webSocketId: String, text: String) {
    val cancellable = context.system.scheduler.schedule(0 milliseconds,
      50 milliseconds)(ChatApp.webServer.webSocketConnections.writeText(text, webSocketId))
  }

  /**
   * Echo the details of the web socket frame that we just received; but in upper case.
   */
  private def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime)

    ChatApp.webServer.webSocketConnections.writeText(ts + " " + event.readText)
  }

}



