package olybot.client.components

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.{ ReactiveElement, ReactiveHtmlElement }
import org.scalajs.dom.html
import scalacss.internal.StyleA

object Button:
  type ButtonType = "submit" | "button" | "reset"

  def apply(
      text: String,
      typ: ButtonType,
      mods: Modifier[ReactiveHtmlElement[html.Button]]*
  ): ReactiveHtmlElement[html.Button] = button(text, `type` := typ, mods)
