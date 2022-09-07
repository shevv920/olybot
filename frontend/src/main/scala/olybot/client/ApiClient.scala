package olybot.client

import com.raquo.laminar.api.L.*
import io.laminext.fetch.Fetch
import zio.json.*

object ApiClient:
  import olybot.client.AppState.storedToken

  def toggleBotEnabled: EventStream[Boolean] =
    storedToken.signal
      .sample(storedToken.signal)
      .flatMap {
        case Some(token) =>
          Fetch
            .put("http://localhost:9000/account/toggle-bot-enabled")
            .addAuthorizationHeader(token)
            .text
            .map(
              _.data.fromJson[Boolean].toOption.getOrElse(false)
            )

        case None => EventStream.fromValue(false)
      }
