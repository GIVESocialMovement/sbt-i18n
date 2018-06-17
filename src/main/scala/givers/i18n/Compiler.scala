package givers.i18n

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}

import play.api.i18n.Messages
import sbt._
import sbt.internal.util.ManagedLogger

case class CompilationEntry(success: Boolean, inputFile: File, filesRead: Set[Path], filesWritten: Set[Path])

object Input {
  def getName(sourceDir: File, inputFile: Path): String = {
    sourceDir.toPath.relativize((inputFile.getParent.toFile / inputFile.toFile.getName).toPath).toString
  }
}

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
  def compileSingle(input: Input, default: Map[String, String]): CompilationEntry = {
    val outputFile = targetDir / input.outputName
    Files.createDirectories(outputFile.getParentFile.toPath)

    val output = serializer(input.locale, default ++ Compiler.parse(input))

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
      val locale = Some(inputFile.toFile.ext).filter(_.nonEmpty).getOrElse(defaultLocale)
      Input(Input.getName(sourceDir, inputFile), inputFile, locale)
    }
    val defaultInput = inputs.find(_.locale == defaultLocale).getOrElse {
      throw new Exception(s"Unable to find messages or messages.$defaultLocale (the default locale).")
    }

    val default = Compiler.parse(defaultInput)

    inputs.map { input => compileSingle(input, default) }
  }
}
