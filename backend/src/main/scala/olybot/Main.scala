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
import olybot.shared.Protocol.*

import java.time.temporal.ChronoUnit

object Main extends ZIOAppDefault:
  private val clientLayers    = ChannelFactory.auto ++ EventLoopGroup.auto()
  private val dataSourceLayer = Quill.DataSource.fromPrefix("database").tapError(e => ZIO.logInfo(e.getMessage))
  private val postgresLayer   = Quill.Postgres.fromNamingStrategy(io.getquill.CamelCase)

  val login = Http.collectZIO[Request] { case req @ Method.POST -> !! / "signin" =>
    for
      body         <- req.body.asString
      request      <- ZIO.fromEither(body.fromJson[Signin.Request]).mapError(new Throwable(_))
      verifyResult <- TwitchAPI.validateToken(request.accessToken)
      _            <- ZIO.logInfo(verifyResult.toString)
      // TODO create or update account
      res = verifyResult match {
              case TokenValidateResult(
                    Some(_),
                    Some(_),
                    Some(_),
                    Some(userId),
                    Some(_),
                    _,
                    _,
                  ) =>
                Security
                  .jwtEncode(userId)
                  .map(enc => Signin.Response.Success(enc))
              case TokenValidateResult(_, _, _, _, _, Some(_), Some(message)) =>
                ZIO.succeed(Signin.Response.Failure(message))
              case _ =>
                ZIO.succeed(Signin.Response.Failure("Unknown error"))
            }
      r <- res
    yield Response.json(r.toJson)
  }

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "health" => ZIO.attempt(Response.ok)
    case Method.GET -> !! / "account" / uuidPath(id) =>
      for acc <- AccountRepo.getById(id)
      yield Response.text(acc.toString)
  }

  private val authedApp: Http[Any, Nothing, AuthedRequest, Response] = Http.collect {
    case AuthedRequest(acc, req @ Method.GET -> !! / "account" / "current") =>
      Response.json(acc.map(acc => User(acc.twitchName, acc.twitchId)).toJson)
  }

  private val authedApps = authedApp @@ Middlewares.authMiddleware

  override def run: ZIO[Any, Nothing, ExitCode] =
    (for
      _   <- Migrations.migrate.as("Migration end").debug
      twc <- TwitchChatClient.twitch.forkDaemon
      twApi <- TwitchAPI.retrieveAppToken
                 .repeat(Schedule.fixed(zio.Duration(1, ChronoUnit.HOURS))) // dev.twitch.tv recommendation
                 .fork
      appPort <- AppConfig.port
      _       <- Server.start(appPort, (login ++ app ++ authedApps) @@ Middlewares.middlewares)
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
