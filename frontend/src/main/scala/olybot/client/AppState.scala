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

  val currentUser: Var[Option[User]] = Var(None)

  val $currentUser: EventStream[Option[User]] = storedToken.signal
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
                case GetCurrent.Response.Success(user) =>
                  currentUser.writer.onNext(Some(user))
                  None
                case GetCurrent.Response.Failure(_) =>
                  currentUser.writer.onNext(None)
                  None
              }
              .flatten
          )

      case None =>
        currentUser.writer.onNext(None)
        EventStream.fromValue(None)
    }
