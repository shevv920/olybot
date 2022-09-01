package olybot

import zhttp.http.{ Body, Headers, Method, Request, URL }
import zhttp.service.Client
import zhttp.service.Client.Config
import zio.{ Ref, ZIO, ZLayer }
import zio.json.*

import scala.deriving.Mirror
import TwitchProtocol.*

trait TwitchAPI:
  val appToken: Ref[Option[TokenRequestResult]]
  def retrieveAppToken: ZIO[Any, Throwable, Unit]
  def validateToken(token: String): ZIO[Any, Throwable, TokenValidateResult]

object TwitchAPI:
  def retrieveAppToken: ZIO[TwitchAPI, Throwable, Unit] = ZIO.serviceWithZIO[TwitchAPI](_.retrieveAppToken)
  def validateToken(token: String): ZIO[TwitchAPI, Throwable, TokenValidateResult] =
    ZIO.serviceWithZIO[TwitchAPI](_.validateToken(token))

  val layer = ZLayer.fromZIO {
    for
      twConfig <- ZIO.service[TwitchConfig]
      client   <- ZIO.service[Client[Any]]
      tokenRef <- Ref.make[Option[TokenRequestResult]](None)
    yield TwitchAPILive(client, twConfig, tokenRef)
  }

final case class TwitchAPILive(
    client: Client[Any],
    twitchConfig: TwitchConfig,
    appToken: Ref[Option[TokenRequestResult]],
) extends TwitchAPI:
  override def retrieveAppToken: ZIO[Any, Throwable, Unit] =
    for
      uri <- ZIO.fromEither(URL.fromString("https://id.twitch.tv/oauth2/token")).orDie
      tokenResult <- client
                       .request(
                         Request(
                           url = uri,
                           method = Method.POST,
                           body = Body.fromString(
                             s"client_id=${twitchConfig.clientId}&client_secret=${twitchConfig.clientSecret}&grant_type=client_credentials"
                           ),
                         ),
                         Config.empty,
                       )

      responseBody <- tokenResult.body.asString
      tokenRequestResult <- ZIO
                              .fromEither(responseBody.fromJson[TokenRequestResult])
                              .orDieWith(new Throwable(_))
      _ <- appToken.set(Some(tokenRequestResult))
      _ <- ZIO.logInfo(s"Set twitch app token with expiration in ${tokenRequestResult.expiresIn} seconds")
    yield ()

  override def validateToken(token: String): ZIO[Any, Throwable, TokenValidateResult] =
    for
      uri <- ZIO.fromEither(URL.fromString("https://id.twitch.tv/oauth2/validate")).orDie
      res <- client.request(
               Request(
                 url = uri,
                 headers = Headers("Authorization", s"OAuth $token"),
               ),
               Config.empty,
             )
      body <- res.body.asString
      res <- ZIO
               .fromEither(body.fromJson[TokenValidateResult])
               .mapError(new Throwable(_))
    yield res

object TwitchProtocol:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]

  final case class TokenRequestResult(
      @jsonField("access_token") accessToken: String,
      @jsonField("expires_in") expiresIn: Long,
      @jsonField("token_type") tokenType: String,
  )

  final case class TokenValidateResult(
      @jsonField("client_id") clientId: Option[String],
      @jsonField("login") login: Option[String],
      @jsonField("scopes") scopes: Option[List[String]],
      @jsonField("user_id") userId: Option[String],
      @jsonField("expires_in") expiresIn: Option[Long],
      @jsonField("status") httpStatus: Option[Long],
      @jsonField("message") message: Option[String],
  )

  object TokenValidateResult:
    given codec: JsonCodec[TokenValidateResult] = DeriveJsonCodec.gen[TokenValidateResult]
