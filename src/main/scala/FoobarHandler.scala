import akka.actor.Actor
import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream}
import java.net.Socket
import org.mashupbots.socko.events.HttpRequestEvent

class FoobarHandler extends Actor {

  override def receive: Actor.Receive = {
    case fooBarRequest: FoobarRequest => {

      val host = fooBarRequest.host
      val port = fooBarRequest.port
      val socket = openSocketWithRemoteServer(host, port)

      val out = getOutputStream(socket)
      val in = getInputStream(socket)

      val bytes = Array[Byte](1, 0, 0, 0, 2, 96, 98)
      out.write(bytes)
      out.flush()


      while (in.available() == 0) {}

      val cmd = in.readByte()
      val error = in.readByte()

      (cmd, error) match {
        case (1, 0) =>
          fooBarRequest.event.response.write("successfully logged in")
      }

    }
  }

  private def toBinary(i: Int, digits: Int = 8) =
    String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

  private def openSocketWithRemoteServer(host: String, port: Int): Socket = {
    new Socket(host, port)
  }

  private def getOutputStream(socket: Socket): DataOutputStream = {
    new DataOutputStream(new BufferedOutputStream(socket.getOutputStream))
  }

  private def getInputStream(socket: Socket): DataInputStream = {
    new DataInputStream(new BufferedInputStream(socket.getInputStream))
  }
}

case class FoobarRequest(event: HttpRequestEvent, host: String, port: Int)