package olybot.client.pages

import com.raquo.laminar.nodes.ReactiveHtmlElement
import io.laminext.fetch.{ Fetch, FetchResponse, ToRequestBody }
import io.laminext.syntax.core.*
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import zio.json.*
import olybot.client.components.*
import olybot.client.Pages.{ Home, Page }
import olybot.client.{ pages, AppState, Pages, StoredVar }
import olybot.shared.protocol
import olybot.shared.protocol.*

object TwitchLogin:
  private val tokenRegexp = """access_token=(\w*)&?.*$""".r

  def element(fragment: String)(using router: Router[Page]): ReactiveHtmlElement[Element] =
    fragment match
      case tokenRegexp(token) =>
        val signinResult = Fetch
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
          .map(s => (_: Option[String]) => s)
        div(
          signinResult --> AppState.storedToken.observer,
          signinResult --> { _ => router.pushState(Home) },
        )
      case _ =>
        div(child.maybe <-- Signal.fromValue {
          router.pushState(Pages.Signin)
          None
        })
