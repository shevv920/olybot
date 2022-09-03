package olybot.client

import com.raquo.airstream.state.Var
import org.scalajs.dom
import olybot.shared.Models.User

final case class AppState private (appToken: Option[String], currentUser: Option[User])

object AppState:
  private val tokenKey = "apiToken"
  def tokenFromStorage = Option(dom.window.localStorage.getItem(tokenKey))

  def init: AppState = AppState(tokenFromStorage, None)
  private val token  = Var(tokenFromStorage)
  val $token         = token.signal

  private def setToken(token: String): Unit    = ???
  private def setCurrentUser(user: User): Unit = ???
