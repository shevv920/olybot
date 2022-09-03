package olybot

import twc.*
import io.getquill.jdbczio.Quill
import olybot.Middlewares.AuthedRequest
import olybot.repositories.AccountRepo
import zio.{ Dequeue, ExitCode, Ref, Schedule, ZIO, ZIOAppDefault, ZLayer }
import zhttp.service.{ ChannelFactory, Client, EventLoopGroup, Server }
import zhttp.http.{ uuid as uuidPath, * }
import zio.json.*
import olybot.TwitchProtocol.TokenValidateResult.*
import olybot.TwitchProtocol.TokenValidateResult
import olybot.shared.Models.User
import olybot.shared.protocol

import java.time.temporal.ChronoUnit

object Main extends ZIOAppDefault:
  private val clientLayers    = ChannelFactory.auto ++ EventLoopGroup.auto()
  private val dataSourceLayer = Quill.DataSource.fromPrefix("database").tapError(e => ZIO.logInfo(e.getMessage))
  private val postgresLayer   = Quill.Postgres.fromNamingStrategy(io.getquill.CamelCase)

  private val routes = (Routes.public ++ Routes.authedApps)

  val app = routes @@ Middlewares.middlewares

  override def run: ZIO[Any, Nothing, ExitCode] =
    (for
      _   <- Migrations.migrate.as("Migration end").debug
      twc <- TwitchChatClient.twitch.forkDaemon
      twApi <- TwitchAPI.retrieveAppToken
                 .repeat(Schedule.fixed(zio.Duration(1, ChronoUnit.HOURS))) // dev.twitch.tv recommendation
                 .fork
      appPort <- AppConfig.port
      _       <- Server.start(appPort, app)
      _       <- twApi.await
      _       <- twc.interrupt
    yield ())
      .provide(
        TwitchClientLive.layer,
        dataSourceLayer,
        postgresLayer,
        Migrations.layer,
        AccountRepo.layer,
        TwitchConfig.layer,
        clientLayers,
        TwitchAPI.layer,
        AppConfig.layer,
        ZLayer.fromZIO(Client.make[Any]),
      )
      .debug
      .exitCode
