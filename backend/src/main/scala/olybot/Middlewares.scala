package olybot

import olybot.repositories.{ Account, AccountRepo }
import olybot.shared.protocol.TokenUser
import zhttp.http.*
import zhttp.http.middleware.{ Cors, HttpMiddleware }
import zhttp.http.Middleware.interceptZIOPatch
import zio.{ Clock, ZIO }
import zio.json.*

object Middlewares:
  val middlewares: Middleware[Any, Nothing, Request, Response, Request, Response] =
    logger ++ Middleware.cors(config =
      Cors
        .CorsConfig()
        .copy(allowedHeaders = Some(Set("*")))
    )

  def logger: HttpMiddleware[Any, Nothing] =
    interceptZIOPatch(req => Clock.nanoTime.map(start => (req.method, req.url, start))) {
      case (response, (method, url, start)) =>
        for
          end <- Clock.nanoTime
          _ <- ZIO
                 .logDebug(s"${response.status.code} $method ${url.encode} ${(end - start) / 1000000}ms")
        yield Patch.empty
    }

  final case class AuthedRequest(user: Option[Account], request: Request)

  def authMiddleware: Middleware[AppConfig & AccountRepo, Throwable, AuthedRequest, Response, Request, Response] =
    Middleware.codecZIO(
      request =>
        for
          mbAuthHeader <-
            ZIO.fromOption(request.headerValue(HeaderNames.authorization)).orElseFail(new Throwable("No token"))
          decoded   <- Security.jwtDecode(mbAuthHeader)
          tokenUser <- ZIO.fromEither(decoded.content.fromJson[TokenUser]).mapError(err => new Throwable(err))
          account   <- AccountRepo.getByTwitchId(tokenUser.userId)
        yield AuthedRequest(account, request),
      response => ZIO.succeed(response),
    )
