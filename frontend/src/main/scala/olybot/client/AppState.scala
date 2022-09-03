package olybot.client

import com.raquo.airstream.state.Var
import org.scalajs.dom
import olybot.shared.Models.User

object AppState:
  val storedToken: StoredVar[Option[String]] = StoredVar("apiKey", None)
