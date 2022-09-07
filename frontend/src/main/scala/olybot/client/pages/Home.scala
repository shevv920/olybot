package olybot.client.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import org.scalajs.dom
import io.laminext.syntax.core.*
import olybot.client.{ ApiClient, AppState, Main, StoredVar }
import olybot.shared.Models.User
import olybot.shared.protocol
import olybot.client.components.*

object Home:
  import olybot.client.styles.given

  def profileData =
    div(
      Styles.profile,
      children <-- AppState.currentUser.signal.map(mbUser =>
        mbUser.fold(Seq.empty)(user =>
          Seq(
            span("Id:"),
            span(user.id.toString),
            span("Twitch id:"),
            span(user.twitchId),
            span("Twitch login:"),
            span(user.name),
            span("Bot enabled:"),
            ToggleButton(
              "Disable",
              "Enable",
              AppState.currentUser.signal.map {
                case Some(u) => u.botEnabled
                case None    => false
              },
              inContext { thisNode =>
                val $click: EventStream[Boolean] =
                  thisNode
                    .events(onClick)
                    .debounce(500)
                    .flatMapTo(ApiClient.toggleBotEnabled)

                $click --> Observer { (v: Boolean) =>
                  AppState.currentUser.writer.onNext(Some(user.copy(botEnabled = v)))
                }
              },
            ),
            span("Bot approved:"),
            div(if user.botApproved then span("✅") else span("❌")),
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
      gridTemplateColumns := "max-content max-content",
      gridTemplateRows    := "auto auto auto",
      gap(8.px),
      color(Colors.fgPrimary),
    )
