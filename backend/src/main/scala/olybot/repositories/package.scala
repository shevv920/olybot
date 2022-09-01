package olybot.repositories

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{ PostgresZioJdbcContext, SnakeCase }
import io.getquill.jdbczio.Quill
import zio.prelude.Assertion.greaterThanOrEqualTo
import zio.prelude.Subtype

type QuillType = Quill.Postgres[io.getquill.CamelCase]

trait Resource:
  import Resource._

  type Limit = Limit.Type
  type Page  = Page.Type

  lazy val defaultLimit: Limit = Limit(10)
  lazy val defaultPage: Page   = Page(1)

object Resource:
  object Limit extends Subtype[Int]:
    override inline def assertion = greaterThanOrEqualTo(0)

  object Page extends Subtype[Int]:
    override inline def assertion = greaterThanOrEqualTo(1)
