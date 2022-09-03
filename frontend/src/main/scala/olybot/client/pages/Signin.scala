package olybot.client.pages

import com.raquo.laminar.api.L.*
import olybot.shared.Values.twitchSigninLink

object Signin:
  val element = div(
    a(
      "Signin with twitch",
      href := twitchSigninLink,
    )
  )
