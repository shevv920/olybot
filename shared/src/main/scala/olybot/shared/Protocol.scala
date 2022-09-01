package olybot.shared

import zio.json.*

import java.util.UUID
import scala.deriving.Mirror

object Protocol:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]

  object Signin:
    final case class Request(accessToken: String)
    sealed trait Response
    object Response:
      final case class Success(token: String)  extends Response
      final case class Failure(reason: String) extends Response
