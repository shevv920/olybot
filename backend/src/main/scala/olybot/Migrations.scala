package olybot

import org.flywaydb.core.Flyway
import zio.{ Task, ZIO, ZLayer }

import javax.sql.DataSource

final case class Migrations(dataSource: DataSource):
  val migrate: Task[Unit] =
    for {
      flyway <- loadFlyway
      _      <- ZIO.attempt(flyway.migrate())
    } yield ()

  private lazy val loadFlyway: Task[Flyway] =
    ZIO.attempt {
      Flyway
        .configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .load()
    }

  /** Removes any added data from the database and reruns the migrations effectively resetting it to its original state.
    */
  val reset: Task[Unit] =
    for {
      _      <- ZIO.debug("RESETTING DATABASE!")
      flyway <- loadFlyway
      _      <- ZIO.attempt(flyway.clean())
      _      <- ZIO.attempt(flyway.migrate())
    } yield ()

object Migrations:
  val layer: ZLayer[DataSource, Nothing, Migrations] =
    ZLayer.fromFunction(Migrations.apply _)

  val migrate: ZIO[Migrations, Throwable, Unit] = ZIO.serviceWithZIO[Migrations](_.migrate)
  val reset: ZIO[Migrations, Throwable, Unit]   = ZIO.serviceWithZIO[Migrations](_.reset)
