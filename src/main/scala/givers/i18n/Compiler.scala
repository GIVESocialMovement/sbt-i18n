package givers.i18n

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}

import play.api.i18n.Messages
import play.api.libs.json.Writes._
import play.api.libs.json.{JsValue, Json}
import sbt._
import sbt.internal.util.ManagedLogger

case class CompilationEntry(success: Boolean, inputFile: File, filesRead: Set[Path], filesWritten: Set[Path])
case class Input(name: String, path: Path, locale: String) {
  val file = path.toFile
  val outputName = Some(file.ext).filter(_.nonEmpty)
    .map { _ => s"$name.js" }
    .getOrElse(s"$name.$locale.js")
}

trait Serializer {
  def apply(locale: String, mappings: Map[String, String]): String
}

object Compiler {
  type Locale = String

  def parse(input: Input): Map[String, String] = {
    Messages.parse(Messages.UrlMessageSource(input.file.toURI.toURL), input.file.getCanonicalPath).fold(throw _, { m => m})
  }
}

class Compiler(
  sourceDir: File,
  targetDir: File,
  logger: ManagedLogger,
  defaultLocale: String,
  serializer: Serializer
) {

  def generate(input: Input, default: Map[String, String]): String = {
    /**
      * We add the option `i18n` to every Vue instantiation. We need to make sure we add the option before
      * VueI18n's `beforeCreate` (https://github.com/kazupon/vue-i18n/blob/dev/dist/vue-i18n.js#L247).
      * So, we use `unshift`.
      */
    s"""
       |"use strict";
       |
       |var vueI18n = new VueI18n({
       |  locale: '${input.locale}',
       |  messages: ${serializer(input.locale, default ++ Compiler.parse(input))}
       |});
       |
       |Vue.options.beforeCreate.unshift(function() {
       |  this.$$options['i18n'] = vueI18n;
       |});
     """.stripMargin.trim
  }

  def compileSingle(input: Input, default: Map[String, String]): CompilationEntry = {
    val outputFile = targetDir / input.outputName
    Files.createDirectories(outputFile.getParentFile.toPath)

    val output = generate(input, default)

    val pw = new PrintWriter(outputFile)
    pw.write(output)
    pw.close()

    CompilationEntry(
      success = true,
      inputFile = input.file,
      filesRead = Set(input.path),
      filesWritten = Set(outputFile.toPath)
    )
  }

  def compile(inputFiles: Seq[Path]): Seq[CompilationEntry] = {
    if (inputFiles.isEmpty) {
      return Seq.empty
    }

    val inputs = inputFiles.map { inputFile =>
      val name = sourceDir.toPath.relativize((inputFile.getParent.toFile / inputFile.toFile.getName).toPath).toString
      val locale = Some(inputFile.toFile.ext).filter(_.nonEmpty).getOrElse(defaultLocale)
      Input(name, inputFile, locale)
    }
    val defaultInput = inputs.find(_.locale == defaultLocale).headOption.getOrElse {
      throw new Exception(s"Unable to find messages.$defaultLocale (the default locale).")
    }

    val default = Compiler.parse(defaultInput)

    inputs.map { input => compileSingle(input, default) }
  }
}
