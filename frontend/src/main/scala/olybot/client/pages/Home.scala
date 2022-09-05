package olybot.client.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import org.scalajs.dom
import io.laminext.syntax.core.*
import olybot.client.{ AppState, Main, StoredVar }
import olybot.shared.Models.User
import olybot.shared.protocol

object Home:
  import AppState.currentUser
  import olybot.client.styles.given

  def profileData =
    div(
      Styles.profile,
      children <-- currentUser.map(mbUser =>
        mbUser.fold(Seq.empty)(user =>
          Seq(
            span("Id:"),
            span(user.id.toString),
            span("Twitch id:"),
            span(user.twitchId),
            span("Twitch login:"),
            span(user.name),
          )
        )
      ),
    )

  val element: ReactiveHtmlElement[Element] = div(
    Styles.container,
    h2("Home", Styles.title),
    profileData,
  )

  import scalacss.DevDefaults.*
  object Styles extends StyleSheet.Inline:
    import dsl.*
    import olybot.client.styles.Colors

    val container: StyleA = style(
      padding(8.px)
    )

    val title: StyleA = style(
      color(Colors.primary),
      backgroundColor(Colors.bgPrimary),
      textAlign.center,
    )

    val profile: StyleA = style(
      display.grid,
      gridTemplateColumns := "auto auto",
      gridTemplateRows    := "auto auto auto",
      gap(8.px),
      color(Colors.fgPrimary),
    )
