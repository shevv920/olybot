package olybot.shared

import zio.json.{ DeriveJsonCodec, JsonCodec }

import java.util.UUID
import scala.deriving.Mirror

object Models:
  inline given [T: Mirror.Of]: JsonCodec[T] = DeriveJsonCodec.gen[T]
  final case class User(id: UUID, name: String, twitchId: String)
