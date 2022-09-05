package olybot.client

import io.laminext.fetch.Fetch
import io.laminext.syntax.core.*
import com.raquo.laminar.api.L.*
import com.raquo.airstream.state.Var
import org.scalajs.dom
import zio.json.*
import olybot.shared.Models.User
import olybot.shared.protocol.User.GetCurrent

object AppState:
  val storedToken: StoredVar[Option[String]] = StoredVar("apiKey", None)

  val currentUser: EventStream[Option[User]] = storedToken.signal
    .sample(storedToken.signal)
    .flatMap {
      case Some(token) =>
        Fetch
          .get("http://localhost:9000/account/current")
          .addAuthorizationHeader(token)
          .text
          .map(
            _.data
              .fromJson[GetCurrent.Response]
              .toOption
              .collect {
                case GetCurrent.Response.Success(user) => Some(user)
                case GetCurrent.Response.Failure(_)    => None
              }
              .flatten
          )
      case None => EventStream.fromValue(None)
    }
