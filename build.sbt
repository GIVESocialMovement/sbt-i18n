lazy val `sbt-i18n` = project in file(".")

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.13",
  "org.mockito" % "mockito-core" % "2.18.3" % Test,
  "com.lihaoyi" %% "utest" % "0.6.3" % Test
)

testFrameworks += new TestFramework("utest.runner.Framework")

addSbtJsEngine("1.2.2")
