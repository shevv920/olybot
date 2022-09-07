package olybot.client

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import io.laminext.syntax.core.*
import org.scalajs.dom
import olybot.client.Pages.Page
import olybot.client.pages.Home
import olybot.client.components.Button
import olybot.shared.Models.User

object Main extends App:
  val CssSettings = scalacss.devOrProdDefaults
  import CssSettings.*
  val rootNode: dom.Element = dom.document.querySelector("#root")
  val headNode: dom.Element = dom.document.querySelector("head")

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
  def app(using router: Router[Page]) =
    div(
      styles.Global.container,
      nav(
        styles.Global.navigate,
        a(Pages.navigateTo(Pages.Home), "Home"),
        child.maybe <-- AppState.$currentUser.map(_ => None),
        child <-- AppState.currentUser.signal.map {
          case Some(u) =>
            Button("Logout", typ = "submit", thisEvents(onClick) --> { _ => AppState.storedToken.set(None) })
          case None =>
            Button("Signin", "button", thisEvents(onClick) --> { _ => Routes.router.pushState(Pages.Signin) })
        },
      ),
      child <-- router.$currentPage.map(Pages.renderPage),
    )

  render(rootNode, app)
