package olybot

import olybot.Middlewares.AuthedRequest
import olybot.TwitchProtocol.TokenValidateResult
import olybot.repositories.AccountRepo
import olybot.shared.protocol
import olybot.shared.Models.User
import zhttp.http.*
import zio.ZIO
import zio.json.*

object Routes:
  val public: Http[AccountRepo & TwitchAPI & AppConfig, Throwable, Request, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "health" =>
      ZIO.succeed(Response.text("ok"))
    case req @ Method.POST -> !! / "signin" =>
      for
        body         <- req.body.asString
        request      <- ZIO.fromEither(body.fromJson[protocol.Signin.Request]).mapError(new Throwable(_))
        verifyResult <- TwitchAPI.validateToken(request.accessToken)
        _            <- ZIO.logInfo(verifyResult.toString)
        res = verifyResult match {
                case tvr: TokenValidateResult if tvr.userId.isDefined && tvr.login.isDefined =>
                  for
                    _ <- AccountRepo.createOrUpdate(tvr.userId.get, tvr.login.get)
                    token <- Security
                               .jwtEncode(tvr.userId.get)
                               .map(enc => protocol.Signin.Response.Success(enc))
                  yield token
                case TokenValidateResult(_, _, _, _, _, Some(_), Some(message)) =>
                  ZIO.succeed(protocol.Signin.Response.Failure(message))
                case _ =>
                  ZIO.succeed(protocol.Signin.Response.Failure("Unknown error"))
              }
        r <- res
      yield Response.json(r.toJson)
  }

  private val authed: Http[Any, Throwable, AuthedRequest, Response] = Http.collect {
    case AuthedRequest(mbAccount, req @ Method.GET -> !! / "account" / "current") =>
      val res = mbAccount match
        case None => protocol.User.GetCurrent.Response.Failure("not found")
        case Some(acc) =>
          protocol.User.GetCurrent.Response
            .Success(User(acc.id, acc.twitchName, acc.twitchId, acc.botEnabled, acc.botApproved))
      Response.json(res.toJson)
  }

  private val authedZIO: Http[AccountRepo, Throwable, AuthedRequest, Response] = Http.collectZIO {
    case AuthedRequest(Some(acc), req @ Method.PUT -> !! / "account" / "toggle-bot-enabled") =>
      for _ <- AccountRepo.updateBotEnabled(acc.id, !acc.botEnabled)
      yield Response.json((!acc.botEnabled).toJson)
  }

  val authedApps = authed @@ Middlewares.authMiddleware ++ authedZIO @@ Middlewares.authMiddleware
