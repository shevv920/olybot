package olybot

import twc.*
import zio.{ Dequeue, Ref, ZIO }
import zhttp.service.Client
import zhttp.http.Method
import zio.json.*

import scala.deriving.Mirror

object TwitchChatClient:
  def twitchEventsHandler(msqQ: Dequeue[IRCEvent], accessToken: String): ZIO[TwitchClient, Throwable, Nothing] =
    (for
      msg <- msqQ.take
      _ <- msg match
             case Connected(address) =>
               for
                 _ <- ZIO.logInfo(s"Connected: $address")
                 _ <- TwitchClient.sendMessage(
                        IRCMessage(Command.CapReq, ":twitch.tv/membership twitch.tv/commands twitch.tv/tags")
                      )
                 _ <- TwitchClient.sendMessage(IRCMessage(Command.Pass, s"oauth:$accessToken"))
                 _ <- TwitchClient.sendMessage(IRCMessage(Command.Nick, "Ostanovijca"))
               yield ()
             case IRCMessageReceived(msg) =>
               ZIO.unit
             case Ping(arg) =>
               TwitchClient.sendMessage(IRCMessage(Command.Pong, arg))
             case _ => ZIO.unit
    yield ()).forever

  val twitch =
    ZIO
      .scoped {
        for
          twitchConfig  <- ZIO.service[TwitchConfig]
          eventsDequeue <- TwitchClient.subscribe
          handler       <- twitchEventsHandler(eventsDequeue, twitchConfig.chatAccessToken).fork
          _             <- ZIO.sleep(zio.Duration.fromSeconds(1))
//          _             <- TwitchClient.connect("irc.chat.twitch.tv", 6667)
          _ <- handler.await
        yield ()
      }
