name := """test-play-project"""
organization := "givers.i18n"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb, SbtI18n)

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice
)

// You can switch the serializer here.
//I18nKeys.i18n / I18nKeys.serializer := givers.i18n.VueI18nSerializer
I18nKeys.i18n / I18nKeys.serializer := givers.i18n.VanillaJsI18nSerializer
