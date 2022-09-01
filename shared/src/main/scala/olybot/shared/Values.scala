package olybot.shared

object Values:
  val appClientId = "8j5tv717b8hwfyfvmt0c88he6d1hhe"
  val redirectURI = "http://localhost:9001/signin/twitch"

  val twitchSigninLink =
    s"https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=$appClientId&redirect_uri=$redirectURI"
