package twc

import scala.util.matching.Regex

sealed trait Command extends Product with Serializable:
  val name: String = this.productPrefix

object Command:
  private val numericCommandRegex: Regex = """(\d{3})""".r

  case object Account extends Command
  case object Join    extends Command
  case object Part    extends Command
  case object Pass    extends Command
  case object Nick    extends Command
  case object User    extends Command
  case object Notice  extends Command
  case object Ping    extends Command
  case object Pong    extends Command
  case object Privmsg extends Command
  case object Kick    extends Command
  case object Quit    extends Command
  case object CapLs extends Command:
    override def toString: String = "CAP LS"
  case object CapReq extends Command:
    override def toString: String = "CAP REQ"
  case object CapEnd                      extends Command
  case object Cap                         extends Command
  case object Who                         extends Command
  case object GlobalUserState             extends Command
  final case class Numeric(value: String) extends Command
  final case class Unknown(value: String) extends Command

  def fromString(str: String): Command =
    str.toLowerCase.capitalize match
      case Account.name           => Account
      case Part.name              => Part
      case Join.name              => Join
      case Pass.name              => Pass
      case Nick.name              => Nick
      case User.name              => User
      case Notice.name            => Notice
      case Ping.name              => Ping
      case Pong.name              => Pong
      case Privmsg.name           => Privmsg
      case Kick.name              => Kick
      case Quit.name              => Quit
      case CapLs.name             => CapLs
      case CapReq.name            => CapReq
      case CapEnd.name            => CapEnd
      case Cap.name               => Cap
      case Who.name               => Who
      case GlobalUserState.name   => GlobalUserState
      case numericCommandRegex(n) => Numeric(n)
      case value                  => Unknown(value)
