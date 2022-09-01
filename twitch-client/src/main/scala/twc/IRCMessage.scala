package twc

import IncomingMessages.IncomingMessage
import zio.{ Hub, Queue, Task, UIO, ZIO }

import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.util.matching.Regex

final case class Prefix(nick: String, username: String, host: String)
final case class IRCMessage(cmd: Command, args: Vector[String], prefix: Option[Prefix] = None)

object IRCMessage {
  private val separator         = " "
  private val messageDelimiter  = "\r\n"
  private val userPrefix: Regex = """:(.*)!(.*)@(.*)""".r
  private val hostPrefix: Regex = """:(.*) """.r

  def apply(cmd: Command, args: String*): IRCMessage =
    if (args.size > 1) // always add ":" to last param
      IRCMessage(cmd, args.toVector.dropRight(1) :+ ":" + args.last, None)
    else
      IRCMessage(cmd, args.toVector, None)

  def toByteArray(msg: IRCMessage): Task[Array[Byte]] =
    ZIO.attempt {
      val str =
        msg.cmd.toString.toUpperCase +
          separator +
          msg.args.mkString(separator) +
          messageDelimiter
      str.getBytes(StandardCharsets.UTF_8)
    }

  def parse(raw: String): Task[IRCMessage] =
    ZIO.attempt {
      val withoutTags = if raw.startsWith("@") then raw.dropWhile(c => c != ' ') else raw
      val (prefix, commandParams) =
        if (withoutTags.startsWith(":"))
          withoutTags.splitAt(withoutTags.indexOf(' ') + 1)
        else ("", withoutTags)
      val (command, params) =
        if (commandParams.indexOf(' ') == -1) (commandParams, "")
        else commandParams.splitAt(commandParams.indexOf(' '))
      val cmd = Command.fromString(command)

      IRCMessage(cmd, parseParams(params.trim), parsePrefix(prefix))
    }

  private def parsePrefix(prefix: String): Option[Prefix] =
    prefix match {
      case userPrefix(nick, user, host) =>
        Some(Prefix(nick, user, host.trim))
      case hostPrefix(host) =>
        Some(Prefix("", "", host))
      case _ => None
    }

  @tailrec
  private def parseParams(remaining: String, cur: Vector[String] = Vector.empty): Vector[String] =
    if remaining.isEmpty then cur
    else if remaining.startsWith(":") then // remaining - last param
      cur :+ remaining.drop(1)
    else
      val split        = remaining.split(" ", 2)
      val (param, rem) = (split(0), if (split.length > 1) split(1) else "")
      parseParams(rem, cur :+ param)

  def parser(q: Queue[IncomingMessage], msgHub: Hub[IRCEvent]): ZIO[Any, Throwable, Unit] =
    for
      raw    <- q.take
      parsed <- parse(raw.toString)
      _      <- msgHub.publish(IRCEvent.fromIrcMessage(parsed))
    yield ()
}
