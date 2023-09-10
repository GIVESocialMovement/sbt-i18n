package givers.i18n

import com.typesafe.sbt.web._
import com.typesafe.sbt.web.incremental._
import sbt.Keys._
import sbt._
import xsbti.{Position, Problem, Severity}


object SbtI18n extends AutoPlugin {

  override def requires = SbtWeb

  override def trigger = AllRequirements

  object autoImport {
    object I18nKeys {
      val i18n = TaskKey[Seq[File]]("i18n", "Run i18n")
      val path = SettingKey[File]("i18nPath", "The parent path of the locale files. It must be a sub-directory of ./conf. Default: ./conf/locale")
      val defaultLocale = SettingKey[String]("i18nDefaultLocale", "The default locale that is also used as a fallback")
      val serializer = SettingKey[Serializer](
        "i18nSerializer",
        "The serializer that converts a set of mappings to the wanted Javascript code. Default to VueI18nSerializer (compatible with https://kazupon.github.io/vue-i18n/)")
    }
  }

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import I18nKeys._

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(Seq(
    i18n / defaultLocale := "en",
    i18n / path := new File("./conf/locale"),
    i18n / serializer := VanillaJsI18nSerializer,
    i18n / excludeFilter := HiddenFileFilter || "_*",
    i18n / includeFilter := new SimpleFileFilter({ f => f.isFile && f.getCanonicalPath.startsWith((i18n / path).value.getCanonicalPath) }),
    i18n / resourceManaged := webTarget.value / "i18n" / "main",
    Assets / managedResourceDirectories += (Assets / i18n / resourceManaged).value,
    Assets / resourceGenerators += Assets / i18n,
    Assets / i18n := task.dependsOn(Assets / WebKeys.webModules).value
  ))

  lazy val task = Def.task {
    val sourceDir = (Compile / resourceDirectory).value
    val targetDir = (Assets / i18n / resourceManaged).value
    val logger = (Assets / streams).value.log
    val vueI18nReporter = (Assets / reporter).value
    val defaultLocaleValue = (i18n / defaultLocale).value
    val serializerValue = (i18n / serializer).value
    val localePath = (i18n / path).value

    val defaultLocaleFile = localePath / "messages"

    val sources = (sourceDir ** ((Assets / i18n / includeFilter).value -- (Assets / i18n / excludeFilter).value)).get

    implicit val fileHasherIncludingOptions = OpInputHasher[File] { f =>
      OpInputHash.hashString(Seq(
        "vueI18n",
        f.getCanonicalPath,
        sourceDir.getAbsolutePath
      ).mkString("--"))
    }

    val results = incremental.syncIncremental((Assets / streams).value.cacheDirectory / "run", sources) { modifiedSources =>
      val startInstant = System.currentTimeMillis

      if (modifiedSources.nonEmpty) {
        logger.info(s"[I18n] Compile on ${modifiedSources.size} changed files")
      } else {
        logger.info(s"[I18n] No changes to compile")
      }

      val compiler = new Compiler(
        sourceDir,
        targetDir,
        logger,
        defaultLocaleValue,
        defaultLocaleFile,
        serializerValue
      )

      // Compile all modified sources at once
      val entries = compiler.compile(modifiedSources.map(_.toPath))

      // Report compilation problems
      CompileProblems.report(
        reporter = vueI18nReporter,
        problems = if (!entries.forall(_.success)) {
          Seq(new Problem {
            override def category() = ""

            override def severity() = Severity.Error

            override def message() = ""

            override def position() = new Position {
              override def line() = java.util.Optional.empty()

              override def lineContent() = ""

              override def offset() = java.util.Optional.empty()

              override def pointer() = java.util.Optional.empty()

              override def pointerSpace() = java.util.Optional.empty()

              override def sourcePath() = java.util.Optional.empty()

              override def sourceFile() = java.util.Optional.empty()
            }
          })
        } else { Seq.empty }
      )

      // Collect OpResults
      val opResults: Map[File, OpResult] = entries
        .map { entry =>
          entry.inputFile -> OpSuccess(entry.filesRead.map(_.toFile), entry.filesWritten.map(_.toFile))
        }
        .toMap

      // Collect the created files
      val createdFiles = entries.flatMap(_.filesWritten.map(_.toFile))

      val endInstant = System.currentTimeMillis

      if (createdFiles.nonEmpty) {
        logger.info(s"[I18n] finished compilation in ${endInstant - startInstant} ms and generated ${createdFiles.size} JS files")
      }

      (opResults, createdFiles)

    }(fileHasherIncludingOptions)

    // Return the dependencies
    (results._1 ++ results._2.toSet).toSeq
  }
}
