package olybot

import io.github.nremond.{ toHex, PBKDF2 }
import olybot.Middlewares.TokenUser
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }
import zio.ZIO
import zio.json.*

import java.time.Clock

object Security:
  implicit val clock: Clock = Clock.systemUTC

  def jwtEncode(userId: String): ZIO[AppConfig, Nothing, String] =
    for
      sk      <- ZIO.serviceWith[AppConfig](_.pwdSecretKey)
      json    <- ZIO.succeed(TokenUser(userId).toJson)
      claim   <- ZIO.succeed(JwtClaim(json).issuedNow.expiresIn(2630000)) // month
      encoded <- ZIO.succeed(Jwt.encode(claim, sk, JwtAlgorithm.HS512))
    yield encoded

  def jwtDecode(token: String): ZIO[AppConfig, Throwable, JwtClaim] =
    for
      sk  <- ZIO.serviceWith[AppConfig](_.pwdSecretKey)
      res <- ZIO.fromTry(Jwt.decode(token, sk, Seq(JwtAlgorithm.HS512)))
    yield res
