lazy val scalachess = Project("scalachess", file(".")).settings(
  name := "scalachess",
  libraryDependencies ++= List(
    "org.specs2"     %% "specs2-core"      % "4.19.2"   % Test,
    "org.specs2"     %% "specs2-cats"      % "4.19.2"   % Test,
    "org.scalameta"  %% "munit"            % "1.0.0-M7" % Test,
    "org.scalacheck" %% "scalacheck"       % "1.17.0"   % Test,
    "org.scalameta"  %% "munit-scalacheck" % "1.0.0-M7" % Test,
    "com.github.lenguyenthanh" % "compression" % "aacf55bea2" % Test, // a fork of lichess compression which public everything so we can use it for testing.
    "com.disneystreaming" %% "weaver-cats"       % "0.8.1" % Test,
    "com.disneystreaming" %% "weaver-scalacheck" % "0.8.1" % Test,
    "com.github.ornicar"  %% "scalalib"          % "9.1.3",
    "joda-time"            % "joda-time"         % "2.12.2",
    "org.typelevel"       %% "cats-core"         % "2.9.0",
    "org.typelevel"       %% "alleycats-core"    % "2.9.0",
    "org.typelevel"       %% "cats-parse"        % "0.3.9"
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  scalacOptions := Seq(
    "-encoding",
    "utf-8",
    // "-rewrite",
    "-source:future-migration",
    "-indent",
    "-explaintypes",
    "-feature",
    "-language:postfixOps"
    // Warnings as errors!
    /* "-Xfatal-warnings" */
  )
)

ThisBuild / organization           := "org.lichess"
ThisBuild / version                := "14.5.8"
ThisBuild / scalaVersion           := "3.2.2"
ThisBuild / licenses += "AGPL-3.0" -> url("https://opensource.org/licenses/AGPL-3.0")

resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"
resolvers += "jitpack" at "https://jitpack.io"

publishTo := Option(Resolver.file("file", new File(sys.props.getOrElse("publishTo", ""))))

lazy val bench = project
  .enablePlugins(JmhPlugin)
  .settings(name := "bench")
  .dependsOn(scalachess, scalachess % "compile->test")
