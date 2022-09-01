package twc

import IncomingMessages.IncomingMessage
import IRCMessage.toByteArray
import OutgoingMessages.OutgoingMessage
import zio.nio.InetSocketAddress
import zio.nio.channels.AsynchronousSocketChannel
import zio.{ Chunk, Dequeue, Hub, Queue, Scope, Trace, ZIO, ZLayer }
import zio.stream.ZStream
import zio.stream.ZPipeline

import java.io.IOException
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.util.matching.Regex

object IncomingMessages:
  opaque type IncomingMessage = String

  object IncomingMessage:
    def apply(s: String): IncomingMessage = s

object OutgoingMessages:
  opaque type OutgoingMessage = Array[Byte]

  object OutgoingMessage:
    def apply(s: Array[Byte]): OutgoingMessage = s
    def apply(s: String): OutgoingMessage      = (s.trim + "\r\n").getBytes

  extension (x: OutgoingMessage)
    inline def toChunk: Chunk[Byte] = Chunk.fromArray(x)
    inline def mkString: String     = new String(x)

trait TwitchClient:
  val incomingMessagesQueue: Queue[IncomingMessage]
  val outgoingMessagesQueue: Queue[OutgoingMessage]
  val eventsHub: Hub[IRCEvent]
  val eventsDequeue: Dequeue[IRCEvent]
  def connect(hostName: => String, port: => Int)(implicit trace: Trace): ZIO[Scope, Throwable, Unit]

object TwitchClient:
  def connect(hostName: => String, port: => Int): ZIO[Scope & TwitchClient, Throwable, Unit] =
    ZIO.serviceWithZIO[TwitchClient](_.connect(hostName, port))

  def sendRawMessage(msg: => String): ZIO[TwitchClient, Nothing, Boolean] =
    ZIO.serviceWithZIO[TwitchClient](_.outgoingMessagesQueue.offer(OutgoingMessage(msg)))

  def sendMessage(msg: => IRCMessage): ZIO[TwitchClient, Throwable, Unit] =
    for
      service <- ZIO.service[TwitchClient]
      arr     <- IRCMessage.toByteArray(msg)
      _       <- service.outgoingMessagesQueue.offer(OutgoingMessage(arr))
    yield ()

  def subscribe: ZIO[Scope & TwitchClient, Nothing, Dequeue[IRCEvent]] =
    ZIO.serviceWithZIO[TwitchClient](_.eventsHub.subscribe)

  val eventsDequeue: ZIO[TwitchClient, Nothing, Dequeue[IRCEvent]] = ZIO.serviceWith[TwitchClient](_.eventsDequeue)

  def splitter(
      stream: ZStream[Any, Throwable, Chunk[Byte]],
      delimiter: String,
  ): ZStream[Any, Throwable, IncomingMessage] =
    stream
      .mapChunks(chunk => chunk.map(c => new String(c.toArray, StandardCharsets.UTF_8)))
      .via(ZPipeline.splitOn(delimiter))
      .map(s => IncomingMessage(s.trim))

final case class TwitchClientLive(
    incomingQ: Queue[IncomingMessage],
    outgoingQ: Queue[OutgoingMessage],
    eventsHub: Hub[IRCEvent],
    eventsDequeue: Dequeue[IRCEvent],
) extends TwitchClient:
  override val incomingMessagesQueue: Queue[IncomingMessage] = incomingQ
  override val outgoingMessagesQueue: Queue[OutgoingMessage] = outgoingQ

  override def connect(hostName: => String, port: => Int)(implicit trace: Trace): ZIO[Scope, Throwable, Unit] =
    for
      channel      <- AsynchronousSocketChannel.open
      address      <- InetSocketAddress.hostNameResolved(hostName, port)
      _            <- ZIO.logInfo(s"Connecting to $address")
      _            <- channel.connect(address)
      remoteOption <- channel.remoteAddress
      remote       <- ZIO.fromOption(remoteOption).orElseFail(new IOException("Failed to connect"))
      _            <- eventsHub.publish(Connected(remote))
      reader       <- reader(channel, incomingQ).fork
      writer       <- writer(channel, outgoingQ).fork
      inMsgParser  <- IRCMessage.parser(incomingQ, eventsHub).forever.fork
    yield ()

  private def reader(channel: => AsynchronousSocketChannel, incomingMessagesQueue: => Queue[IncomingMessage]) =
    TwitchClient
      .splitter(
        ZStream.repeatZIO(channel.readChunk(TwitchClientLive.maxMessageLength)),
        TwitchClientLive.messageDelimiter,
      )
      .tap(msg => ZIO.logInfo(s"Received: ${msg.toString.trim}"))
      .foreach(incomingMessagesQueue.offer)
      .forever

  private def writer(channel: => AsynchronousSocketChannel, outgoingMessagesQueue: => Queue[OutgoingMessage]) =
    (for
      msg <- outgoingMessagesQueue.take
      _   <- channel.writeChunk(msg.toChunk)
      _   <- ZIO.logInfo(s"Written: ${msg.mkString.trim}")
    yield ()).forever

object TwitchClientLive:
  val maxMessageLength = 512
  val messageDelimiter = "\r\n"

  val layer: ZLayer[Any, Nothing, TwitchClient] =
    ZLayer.scoped {
      for
        incQ   <- Queue.unbounded[IncomingMessage]
        outQ   <- Queue.unbounded[OutgoingMessage]
        msgHub <- Hub.unbounded[IRCEvent]
        dq     <- msgHub.subscribe
      yield TwitchClientLive(incQ, outQ, msgHub, dq)
    }
