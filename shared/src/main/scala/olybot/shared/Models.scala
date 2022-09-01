package olybot.shared

import zio.json.{ DeriveJsonCodec, JsonCodec }

import scala.deriving.Mirror

object Models:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]
  final case class User(name: String, twitchId: String)
