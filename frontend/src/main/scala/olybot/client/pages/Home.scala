package olybot.client.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import io.laminext.syntax.core.*
import io.laminext.fetch.Fetch
import olybot.client.Main
import olybot.shared.Models.User
import olybot.client.StoredVar
import org.scalajs.dom
import zio.json.*

import scala.concurrent.duration.*
import olybot.shared.protocol

object Home:
  val storedT: StoredVar[Option[String]] = StoredVar("key", None)

  val currentUser: EventStream[Option[User]] = storedT.signal
    .sample(storedT.signal)
    .flatMap {
      case Some(token) =>
        Fetch
          .get("http://localhost:9000/account/current")
          .addAuthorizationHeader(token)
          .text
          .map(
            _.data
              .fromJson[protocol.User.GetCurrent.Response]
              .toOption
              .collect {
                case protocol.User.GetCurrent.Response.Success(user)   => Some(user)
                case protocol.User.GetCurrent.Response.Failure(reason) => None
              }
              .flatten
          )
      case None => EventStream.empty
    }

  import olybot.client.styles.given
  val element: ReactiveHtmlElement[Element] = div(
    h2("Home", Styles.title),
    div(
      div(child.maybe <-- currentUser.map(user => user.map(_.name))),
      div(child.maybe <-- currentUser.map(user => user.map(_.twitchId))),
    ),
  )

  import scalacss.DevDefaults.*
  object Styles extends StyleSheet.Inline {
    import dsl.*
    import olybot.client.styles.Colors

    val title: StyleA = style(
      color(Colors.primary),
      backgroundColor(Colors.bgPrimary),
      textAlign.center,
    )
  }
