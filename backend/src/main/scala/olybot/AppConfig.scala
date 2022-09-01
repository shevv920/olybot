package olybot

import zio.ZIO
import zio.config.*
import zio.config.derivation.name
import zio.ZLayer
import zio.config.magnolia.descriptor

final case class AppConfig(
    @name("APP_HOST") host: String,
    @name("APP_PORT") port: Int,
    @name("PWD_SK") pwdSecretKey: String,
)

object AppConfig {
  private val configDescriptor = descriptor[AppConfig]

  def port: ZIO[AppConfig, Nothing, Int] = ZIO.serviceWith[AppConfig](_.port)

  val layer: ZLayer[Any, ReadError[String], AppConfig] = ZConfig
    .fromSystemEnv(configDescriptor)
    .orElse(
      ZConfig.fromMap(
        Map(
          "host"         -> "localhost",
          "port"         -> "9000",
          "pwdSecretKey" -> "PASSWORD_SECRET_KEY",
        ),
        configDescriptor,
      )
    )
    .tapError(err => ZIO.logError(s"Config load error: $err"))
}
