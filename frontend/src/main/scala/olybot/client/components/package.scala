package olybot.client.components

import com.raquo.laminar.api.L.*
import com.raquo.laminar.modifiers.ChildInserter
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{ html, HTMLElement }

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random

extension (x: ReactiveHtmlElement[html.Button]) {
  def disableBy(signal: Signal[Boolean]) = x.amend(disabled <-- signal)
}

trait CustomComponent:
  def elem: Modifier[ReactiveHtmlElement[HTMLElement]]

given Conversion[CustomComponent, Modifier[ReactiveHtmlElement[HTMLElement]]] = _.elem

def ToggleButton(
    onValue: Node,
    offValue: Node,
    value: Signal[Boolean],
    mods: Modifier[Button]*
): Button =
  button(
    typ := "button",
    child <-- value.map(v => if v then onValue else offValue),
    mods,
  )
