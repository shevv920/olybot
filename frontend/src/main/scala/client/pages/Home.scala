package client.pages

import client.Main
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import io.laminext.syntax.core.*
import olybot.shared.Models.User

object Home {
  import client.styles.given

//  ApiClient.apiToken.updateObserver. TODO bind apiToken value to observer which fetches current account

  val element: ReactiveHtmlElement[Element] = div(
    h2("Home", Styles.title)
  )

  import scalacss.DevDefaults.*
  object Styles extends StyleSheet.Inline {
    import dsl.*
    import client.styles.Colors

    val title: StyleA = style(
      color(Colors.primary),
      backgroundColor(Colors.bgPrimary),
      textAlign.center,
    )
  }
}
