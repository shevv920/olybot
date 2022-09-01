val scala3Version    = "3.1.3"
val zioConfigVersion = "3.0.2"
val quillVersion     = "4.2.0"
val zioNioVersion    = "2.0.0+1-7c803ecb-SNAPSHOT"
val zioVersion       = "2.0.1"
val zhttpVersion     = "2.0.0-RC11"
val zioJsonVersion   = "0.3.0-RC10"
val flywayVersion    = "9.1.6"
val laminextVersion  = "0.14.3"

ThisBuild / scalaVersion := scala3Version
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
)

lazy val twitchClient = project
  .in(file("./twitch-client"))
  .settings(
    name         := "twc",
    scalaVersion := "3.1.3",
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-nio"      % zioNioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test"     % zioVersion % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  )

lazy val backend = project
  .in(file("./backend"))
  .settings(
    name         := "backend",
    scalaVersion := "3.1.3",
    libraryDependencies ++= Seq(
      "org.postgresql"        % "postgresql"          % "42.4.2",
      "org.slf4j"             % "slf4j-nop"           % "1.7.36",
      "io.d11"               %% "zhttp"               % zhttpVersion,
      "io.getquill"          %% "quill-jdbc-zio"      % quillVersion,
      "com.github.jwt-scala" %% "jwt-core"            % "9.1.0",
      "io.github.nremond"    %% "pbkdf2-scala"        % "0.7.0",
      "org.flywaydb"          % "flyway-core"         % flywayVersion,
      "dev.zio"              %% "zio-config"          % zioConfigVersion,
      "dev.zio"              %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio"              %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio"              %% "zio-prelude"         % "1.0.0-RC15",
      "dev.zio"              %% "zio"                 % zioVersion,
      "dev.zio"              %% "zio-json"            % zioJsonVersion,
      "dev.zio"              %% "zio-test"            % zioVersion % Test,
      "dev.zio"              %% "zio-test-sbt"        % zioVersion % Test,
      "dev.zio"              %% "zio-test-magnolia"   % zioVersion % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  )
  .dependsOn(twitchClient, shared.jvm)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("./shared"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "3.1.3",
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % zioJsonVersion
    ),
  )

lazy val frontend = project
  .in(file("./frontend"))
  .dependsOn(shared.js)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.raquo"                    %%% "laminar"         % "0.14.2",
      "com.raquo"                    %%% "waypoint"        % "0.5.0",
      "dev.zio"                      %%% "zio-json"        % zioJsonVersion,
      "io.laminext"                  %%% "fetch"           % laminextVersion,
      "io.laminext"                  %%% "validation-core" % laminextVersion,
      "io.laminext"                  %%% "ui"              % laminextVersion,
      "com.github.japgolly.scalacss" %%% "core"            % "1.0.0",
    ),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig in (Compile, fastOptJS) ~= {
      _.withESFeatures(
        _.withAvoidLetsAndConsts(false)
          .withAvoidClasses(false)
          .withESVersion(org.scalajs.linker.interface.ESVersion.ES2021)
      ).withPrettyPrint(true)
        .withClosureCompilerIfAvailable(false)
        .withOptimizer(false)
        .withCheckIR(true)
    },
  )

lazy val root = project
  .in(file("."))
  .settings(
    name         := "olybot",
    version      := "0.1.0",
    scalaVersion := scala3Version,
  )
  .aggregate(backend, twitchClient, frontend)

lazy val fastOptCompileCopy = taskKey[Unit]("")

lazy val htmlPath = "frontend/resources/index.html"

fastOptCompileCopy := {
  val source    = (frontend / Compile / fastOptJS).value.data
  val sourceMap = source.getParentFile / (source.getName + ".map")
  val hash      = Hash.toHex(Hash(source))
  val htmlFile  = baseDirectory.value / htmlPath
  val srcHtml   = IO.readBytes(htmlFile)
  val htmlWithScript =
    new String(srcHtml).replaceAll("script-.*\\.js", s"script-dev-$hash.js").getBytes
  IO.write(source.getParentFile / "with-html" / "index.html", htmlWithScript)

  IO.copyFile(
    source,
    source.getParentFile / "with-html" / s"script-dev-$hash.js",
  )
  IO.copyFile(
    sourceMap,
    source.getParentFile / "with-html" / s"frontend-fastopt.js.map",
  )
}
