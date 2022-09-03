package olybot.shared.protocol

import olybot.shared.Models.User
import zio.json.{ DeriveJsonCodec, JsonCodec }

import scala.deriving.Mirror

object Signin:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]

  final case class Request(accessToken: String)
  sealed trait Response
  object Response:
    final case class Success(token: String)  extends Response
    final case class Failure(reason: String) extends Response

object User:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]

  object GetCurrent:
    sealed trait Response extends Product with Serializable
    object Response:
      final case class Success(user: User)     extends Response
      final case class Failure(reason: String) extends Response

final case class TokenUser(userId: String)

object TokenUser:
  given codec: JsonCodec[TokenUser] = DeriveJsonCodec.gen[TokenUser]
