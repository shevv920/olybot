package olybot.client

import com.raquo.domtypes.generic.codecs.{ Codec, StringAsIsCodec }
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.*
import org.scalajs.dom.html.Element
import org.scalajs.dom
import urldsl.errors.DummyError
import urldsl.language.PathQueryFragmentRepr
import zio.json.*
import io.laminext.syntax.core.*

object Routes:
  import Pages.*

  val twSigninRoute: Route[TwitchSignin, FragmentPatternArgs[Unit, Unit, String]] =
    Route.withFragment(
      encode = page => FragmentPatternArgs(path = (), query = (), fragment = page.accessToken),
      decode = args => TwitchSignin(args.fragment),
      pattern = (root / "signin" / "twitch" / endOfSegments) withFragment fragment[String],
    )

  val signinRoute: Route[Signin.type, Unit] = Route.static(Signin, root / "signin" / endOfSegments)
  val homeRoute: Route[Home.type, Unit]     = Route.static(Home, root / endOfSegments)
  val logoutRoute: Route[Logout.type, Unit] = Route.static(Logout, root / "logout" / endOfSegments)

  given JsonCodec[Page] = DeriveJsonCodec.gen[Page]

  val router = new Router[Page](
    routes = List(twSigninRoute, signinRoute, homeRoute, logoutRoute),
    getPageTitle = _.title,
    serializePage = page => page.toJson,
    deserializePage = pageStr => pageStr.fromJson[Page].getOrElse(NotFound),
    routeFallback = _ => NotFound,
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner,
  )
