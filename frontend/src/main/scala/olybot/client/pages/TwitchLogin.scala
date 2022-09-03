package olybot.client.pages

import olybot.client.components.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import io.laminext.fetch.{ Fetch, FetchResponse, ToRequestBody }
import io.laminext.syntax.core.*
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import olybot.client.Pages.{ Home, Page }
import olybot.client.Pages
import olybot.client.pages
import zio.json.*
import olybot.shared.protocol
import olybot.shared.protocol.*
import olybot.client.StoredVar

object TwitchLogin:
  private val tokenRegexp                  = """access_token=(\w*)&?.*$""".r
  private val s: StoredVar[Option[String]] = StoredVar("somekey", None)

  def element(fragment: String)(using router: Router[Page]): ReactiveHtmlElement[Element] =
    fragment match
      case tokenRegexp(token) =>
        div(
          Fetch
            .post(
              "http://localhost:9000/signin",
              body = ToRequestBody.stringRequestBody(protocol.Signin.Request(token).toJson),
            )
            .text
            .map(
              _.data
                .fromJson[protocol.Signin.Response]
                .toOption
                .collect {
                  case protocol.Signin.Response.Success(t)      => Some(t)
                  case protocol.Signin.Response.Failure(reason) => None
                }
                .flatten
            )
            .map(s => (_: Option[String]) => s) --> pages.Home.storedT.observer,
          Pages.navigateTo(Home),
          div(child.text <-- pages.Home.storedT.signal.map(_.toJson)),
        )
      case _ => div(Pages.navigateTo(Pages.Signin))
