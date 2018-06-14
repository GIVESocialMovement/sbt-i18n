lazy val `sbt-i18n` = project in file(".")

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.13"
)

addSbtJsEngine("1.2.2")
