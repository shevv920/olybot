package olybot.client

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import io.laminext.syntax.core.*
import olybot.client.Pages.Page
import olybot.client.pages.Home
import olybot.shared.Models.User
import org.scalajs.dom

object Main extends App:
  val CssSettings = scalacss.devOrProdDefaults
  import CssSettings.*
  val rootNode: dom.Element = dom.document.querySelector("#root")
  val headNode: dom.Element = dom.document.querySelector("head")

  given appState: AppState   = AppState.init
  given router: Router[Page] = Routes.router

  render(
    headNode,
    styleTag(
      styles.Default.render[String],
      styles.Global.render[String],
      pages.Home.Styles.render[String],
    ),
  )

  import styles.given
  def app(using appState: AppState)(using router: Router[Page]) =
    div(
      styles.Global.container,
      nav(
        styles.Global.navigate,
        a(Pages.navigateTo(Pages.Home), "Home"),
      ),
      child <-- router.$currentPage.map(Pages.renderPage),
    )

  render(rootNode, app)
