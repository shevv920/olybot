package client.pages

import client.components.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import io.laminext.fetch.{ Fetch, FetchResponse }
import io.laminext.syntax.core.*
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L.*
import zio.json.*
import olybot.shared.Protocol.*

object TwitchLogin {
  private val tokenRegexp = """access_token=(\w*)&?.*$""".r

  def element(fragment: String): ReactiveHtmlElement[Element] = ???

}
