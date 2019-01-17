lazy val `sbt-i18n` = project in file(".")

enablePlugins(SbtWebBase)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.21",
  "org.mockito" % "mockito-core" % "2.18.3" % Test,
  "com.lihaoyi" %% "utest" % "0.6.3" % Test
)

organization := "givers.i18n"
name := "sbt-i18n"

scalaVersion := "2.12.8"

publishMavenStyle := true

bintrayOrganization := Some("givers")

bintrayRepository := "maven"

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))

testFrameworks += new TestFramework("utest.runner.Framework")

addSbtJsEngine("1.2.3")
