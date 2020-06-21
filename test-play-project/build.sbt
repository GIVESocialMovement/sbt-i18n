name := """test-play-project"""
organization := "givers.i18n"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb, SbtI18n)

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  guice
)
