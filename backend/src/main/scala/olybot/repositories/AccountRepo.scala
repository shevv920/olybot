package olybot.repositories

import io.getquill.jdbczio.Quill
import zio.{ ZIO, ZLayer }

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import io.getquill.{ EntityQuery, Quoted }
import zio.ULayer

final case class Account(
    id: UUID,
    twitchId: String,
    twitchName: String,
    botEnabled: Boolean = false,
    botApproved: Boolean = false,
)

trait AccountRepo extends Resource:
  def getById(id: UUID): ZIO[Any, SQLException, Option[Account]]
  def getByTwitchId(twitchId: String): ZIO[Any, SQLException, Option[Account]]
  def get(limit: Limit, page: Page): ZIO[Any, SQLException, List[Account]]
  def createOrUpdate(twitchId: String, twitchName: String): ZIO[Any, SQLException, Boolean]

object AccountRepo:
  def getById(id: UUID): ZIO[AccountRepo, SQLException, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepo](_.getById(id))

  def getByTwitchId(twitchId: String): ZIO[AccountRepo, SQLException, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepo](_.getByTwitchId(twitchId))

  def createOrUpdate(twitchId: String, twitchName: String): ZIO[AccountRepo, SQLException, Boolean] =
    ZIO.serviceWithZIO[AccountRepo](_.createOrUpdate(twitchId, twitchName))

  val layer =
    ZLayer
      .fromFunction(AccountRepoLive.apply _)

final case class AccountRepoLive(quill: QuillType) extends AccountRepo:
  import io.getquill.*
  import quill.*

  inline def accounts: Quoted[EntityQuery[Account]] =
    quote {
      querySchema[Account]("accounts")
    }

  override def get(limit: Limit, page: Page): ZIO[Any, SQLException, List[Account]] =
    run(
      quote(accounts.drop(lift((page - 1) * limit)).take(lift(limit.toInt)))
    )

  override def getById(id: UUID): ZIO[Any, SQLException, Option[Account]] =
    run(
      accounts
        .filter(acc => acc.id == lift(id))
    )
      .map(_.headOption)

  override def getByTwitchId(twitchId: String): ZIO[Any, SQLException, Option[Account]] =
    inline def q =
      quote {
        accounts
          .filter(acc => acc.twitchId == lift(twitchId))
      }
    run(q).map(_.headOption)

  override def createOrUpdate(twitchId: String, twitchName: String): ZIO[Any, SQLException, Boolean] =
    inline def insertOrUpdate =
      quote {
        accounts
          .insert(_.twitchId -> lift(twitchId), _.twitchName -> lift(twitchName))
          .onConflictUpdate(_.twitchId)((a, _) => a.twitchName -> lift(twitchName))
      }

    run(insertOrUpdate).map(_ > 0)
