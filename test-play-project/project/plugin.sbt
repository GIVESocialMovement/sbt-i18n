lazy val root = Project("plugins", file(".")).aggregate(sbtI18n).dependsOn(sbtI18n)

lazy val sbtI18n = RootProject(file("./..").getCanonicalFile.toURI)

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.2")
