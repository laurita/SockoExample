import akka.util.Timeout
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.actor.{Props, ActorLogging, Actor}
import scala.concurrent._
import duration._
import ExecutionContext.Implicits._
import scala.util.parsing.json.JSON
import akka.pattern.ask
import akka.util.Timeout

case class Push(text: String)

/**
 * Web Socket processor for chatting
 */
class WebSocketRequestHandler(webSocketId: String) extends Actor with ActorLogging {

  log.info("WebSocketRequestHandler created")

  override def receive = notLoggedIn

  def notLoggedIn: Receive = {
    case event: WebSocketFrameEvent =>
      // get JSON from response data
      val jsonStr = event.readText()

      // login remotely, get response
      implicit val timeout = Timeout(3.seconds)
      val successF = context.system.actorOf(Props[SocketWriter]) ? Login(jsonStr)
      val success = Await.result(successF, 3 seconds).asInstanceOf[Boolean]

      // if OK, send response back to WebSocketRequestHandler
      if (success) {
        context.become(loggedIn)
        val jsonResString: String = "{\"action\":\"login\", \"params\": {\"success\":\"true\"}}"
        ChatApp.webServer.webSocketConnections.writeText(jsonResString, webSocketId)
      }

  }

  def loggedIn: Receive = {

      /*
      case Push(text) =>
        log.info("Push")
        pushTwice(webSocketId, text)
      */

      case event: WebSocketFrameEvent =>
        // Echo web socket text frames


        writeWebSocketResponse(event)
      //context.stop(self)
  }

  /**
   * push messages every second
   */
  /*
  private def pushTwice(webSocketId: String, text: String) {
    val cancellable = context.system.scheduler.schedule(0 milliseconds,
      50 milliseconds)(ChatApp.webServer.webSocketConnections.writeText(text, webSocketId))
  }
  */

  /**
   * Echo the details of the web socket frame that we just received; but in upper case.
   */
  def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime)

    ChatApp.webServer.webSocketConnections.writeText(ts + " " + event.readText)
  }

  private def getUsernameFromJSON(event: WebSocketFrameEvent): String = {
    var username = try {
      val jsonString = event.readText()
      val json: Option[Any] = JSON.parseFull(jsonString)
      val map: Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
      map.get("username").toString
    } catch {
      case e => ""

    }
    username
  }

}



