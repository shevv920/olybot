package twc

import zio.nio.SocketAddress

sealed trait IRCEvent                                         extends Product with Serializable
final case class IRCMessageReceived(msg: IRCMessage)          extends IRCEvent
final case class Connected(socketAddress: SocketAddress)      extends IRCEvent
final case class Disconnected(socketAddress: SocketAddress)   extends IRCEvent
final case class PrivateMessage(target: String, msg: String)  extends IRCEvent
final case class ChannelMessage(channel: String, msg: String) extends IRCEvent
final case class Ping(arg: String)                            extends IRCEvent
final case class Pong(arg: String)                            extends IRCEvent

object IRCEvent:
  def fromIrcMessage(ircMessage: IRCMessage): IRCEvent =
    ircMessage match
      case IRCMessage(Command.Privmsg, target +: rest, _) if target.startsWith("#") =>
        ChannelMessage(target, rest.mkString(""))
      case IRCMessage(Command.Privmsg, target +: rest, _) =>
        PrivateMessage(target, rest.mkString(""))
      case IRCMessage(Command.Ping, args, _) =>
        Ping(args.mkString(""))
      case IRCMessage(Command.Pong, args, _) =>
        Pong(args.mkString(""))
      case _ => IRCMessageReceived(ircMessage)
