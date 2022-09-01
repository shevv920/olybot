package client.components

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import scalacss.internal.StyleA

object Button:
  type ButtonType = "submit" | "button" | "reset"

  def apply(text: String, typ: ButtonType): ReactiveHtmlElement[org.scalajs.dom.html.Button] =
    button(text, `type` := typ)
