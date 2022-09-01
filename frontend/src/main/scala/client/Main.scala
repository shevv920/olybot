package client

import client.pages.Home
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import olybot.shared.Models.User
import org.scalajs.dom

object Main extends App {
  val CssSettings = scalacss.devOrProdDefaults
  import CssSettings.*
  val rootNode: dom.Element = dom.document.querySelector("#root")
  val headNode: dom.Element = dom.document.querySelector("head")

  render(
    headNode,
    styleTag(
      styles.Default.render[String],
      styles.Global.render[String],
      pages.Home.Styles.render[String],
    ),
  )
  render(rootNode, Router.container)
}
