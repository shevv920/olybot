package olybot

import zio.config.*
import zio.config.magnolia.*
import zio.config.magnolia.{ descriptor, Descriptor }
import zio.ZIO

final case class TwitchConfig(
    @name("TWITCH_CLIENT_ID") clientId: String,
    @name("TWITCH_CLIENT_SECRET") clientSecret: String,
    @name("TWITCH_CHAT_ACCESS_TOKEN") chatAccessToken: String,
)

object TwitchConfig:
  private val twDescriptor = descriptor[TwitchConfig]

  val layer = ZConfig.fromSystemEnv(twDescriptor).tapError(e => ZIO.logError(e.toString))

  def clientId        = ZIO.serviceWith[TwitchConfig](_.clientId)
  def clientSecret    = ZIO.serviceWith[TwitchConfig](_.clientSecret)
  def chatAccessToken = ZIO.serviceWith[TwitchConfig](_.chatAccessToken)
