package olybot.client

import olybot.client.components.*
import com.raquo.laminar.CollectionCommand
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import io.laminext.fetch.{ Fetch, FetchResponse }
import io.laminext.syntax.core.*
import org.scalajs.dom.html.Element
import org.scalajs.dom
import com.raquo.waypoint.Router
import zio.json.*

import scala.util.{ Failure, Success }

object Pages:
  sealed trait Page extends Product with Serializable {
    val title: String = s"[Oly bot]: ${this.productPrefix}"
  }

  case object Home                             extends Page
  case class TwitchSignin(accessToken: String) extends Page
  case object Signin                           extends Page
  case object NotFound                         extends Page
  case object Logout                           extends Page

  def renderPage(page: Page)(using router: Router[Page]): ReactiveHtmlElement[Element] =
    page match {
      case Home                      => pages.Home.element
      case Signin                    => pages.Signin.element
      case TwitchSignin(accessToken) => pages.TwitchLogin.element(accessToken)
      case NotFound                  => div("page not found")
      case Logout =>
        div(
          navigateTo(Signin),
          None,
        )
    }

  inline def navigateTo(page: Page)(using router: Router[Page]): Binder[HtmlElement] =
    Binder { el =>
      val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

      if (isLinkElement) {
        el.amend(href(router.absoluteUrlForPage(page)))
      }
      (onClick
        .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
        .preventDefault
        --> (_ => router.pushState(page))).bind(el)
    }
