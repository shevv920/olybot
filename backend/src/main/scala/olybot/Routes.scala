package olybot

import olybot.TwitchProtocol.TokenValidateResult
import olybot.shared.Protocol.*
import zhttp.http.*
import zio.ZIO
import zio.json.*

object Routes:
  val public = Http.collect[Request] { case Method.GET -> !! / "health" =>
    Response.text("ok")
  }
