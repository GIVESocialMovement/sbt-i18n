package givers.i18n

import java.io.{FileNotFoundException, PrintWriter}
import java.nio.file.Files

import helpers.BaseSpec
import sbt.internal.util.ManagedLogger
import utest._

import scala.io.Source

object CompilerSpec extends BaseSpec {
  val tests = Tests {
    import sbt._

    val serializer = mock[Serializer]
    when(serializer.apply(any(), any())).thenReturn("someOutput")
    val logger = mock[ManagedLogger]
    val sourceDir = Files.createTempDirectory("sourceDir")
    val targetDir = Files.createTempDirectory("targetDir")
    val defaultLocaleFile = {
      val tmp = Files.createTempFile("messages", "")
      val pw = new PrintWriter(tmp.toFile)
      pw.write(
        """
          |test.map.a = default-a
          |test.map.c = default-c
        """.stripMargin)
      pw.close()
      tmp.toFile
    }
    val compiler = new Compiler(
      sourceDir = sourceDir.toFile,
      targetDir = targetDir.toFile,
      logger = logger,
      defaultLocale = "en-GB",
      defaultLocaleFile = defaultLocaleFile,
      serializer = serializer
    )
    val inputContent = """
      |test.string = sss
      |test.map.a = aaa
      |test.map.b = bbb
    """.stripMargin

    def makeInput(fileSuffix: String, locale: String, content: String = inputContent): Input = {
      val inputPath = Files.createTempFile(sourceDir, "messages", fileSuffix)
      val inputName = Input.getName(sourceDir.toFile, inputPath)
      val input = Input(inputName, inputPath, locale)
      val pw = new PrintWriter(input.path.toFile)
      pw.write(content)
      pw.close()
      input
    }

    'parse - {
      Compiler.parse(defaultLocaleFile) ==> Map(
        "test.map.a" -> "default-a",
        "test.map.c" -> "default-c"
      )
    }

    'compile - {
      'empty - {
        compiler.compile(Seq.empty) ==> Seq.empty
        verifyZeroInteractions(serializer)
      }

      'errorWhenNoDefault - {
        val invalidDefault = new Compiler(
          sourceDir = sourceDir.toFile,
          targetDir = targetDir.toFile,
          logger = logger,
          defaultLocale = "en-GB",
          defaultLocaleFile = new File("/random-folder-that-does-not-exist/messages"),
          serializer = serializer
        )
        val ex = intercept[FileNotFoundException] {
          invalidDefault.compile(Seq(makeInput(".de", "de").path))
        }
        ex.getMessage ==> "/random-folder-that-does-not-exist/messages (No such file or directory)"
        verifyZeroInteractions(serializer)
      }

      'succeed - {
        val noExtContent =
          """
            |test.map.a = aaa2
            |test.map.c = ccc
          """.stripMargin
        val inputFiles = Seq(
          makeInput(".de", "de", inputContent).path,
          makeInput("", "en-GB", noExtContent).path
        )
        val entries = compiler.compile(inputFiles)

        entries.size ==> 2
        entries.head.success ==> true
        entries.head.inputFile.getCanonicalPath ==> inputFiles.head.toFile.getCanonicalPath
        entries.head.filesWritten.size ==> 1
        Source.fromFile(entries.head.filesWritten.head.toFile).mkString ==> "someOutput"

        entries(1).success ==> true
        entries(1).inputFile.getCanonicalPath ==> inputFiles(1).toFile.getCanonicalPath
        entries(1).filesWritten.size ==> 1
        Source.fromFile(entries(1).filesWritten.head.toFile).mkString ==> "someOutput"

        verify(serializer).apply(
          locale = "de",
          mappings = Map(
            "test.string" -> "sss",
            "test.map.a" -> "aaa",
            "test.map.b" -> "bbb",
            "test.map.c" -> "default-c"
          )
        )
        verify(serializer).apply(
          locale = "en-GB",
          mappings = Map(
            "test.map.a" -> "aaa2",
            "test.map.c" -> "ccc"
          )
        )
        ()
      }
    }

    'compileSingle - {
      'nonDefault - {
        val input = makeInput(".de", "de")
        val expectedOutputFile = targetDir.toFile / s"${input.name}.js"
        expectedOutputFile.getCanonicalPath.contains(".de") ==> true

        val entry = compiler.compileSingle(input, Map("test.map.a" -> "aaa2", "test.map.c" -> "ccc"))
        entry.success ==> true
        entry.inputFile.getCanonicalPath ==> input.path.toFile.getCanonicalPath
        entry.filesRead.map(_.toFile.getCanonicalPath) ==> Set(input.path.toFile.getCanonicalPath)
        entry.filesWritten.map(_.toFile.getCanonicalPath) ==> Set(expectedOutputFile.getCanonicalPath)

        Source.fromFile(expectedOutputFile).mkString ==> "someOutput"

        verify(serializer).apply(
          locale = "de",
          mappings = Map(
            "test.string" -> "sss",
            "test.map.a" -> "aaa",
            "test.map.b" -> "bbb",
            "test.map.c" -> "ccc"
          )
        )
        ()
      }

      'default - {
        val input = makeInput("", "de")
        val expectedOutputFile = targetDir.toFile / s"${input.name}.de.js"

        val entry = compiler.compileSingle(input, Map("test.map.a" -> "aaa2", "test.map.c" -> "ccc"))
        entry.success ==> true
        entry.inputFile.getCanonicalPath ==> input.path.toFile.getCanonicalPath
        entry.filesRead.map(_.toFile.getCanonicalPath) ==> Set(input.path.toFile.getCanonicalPath)
        entry.filesWritten.map(_.toFile.getCanonicalPath) ==> Set(expectedOutputFile.getCanonicalPath)

        Source.fromFile(expectedOutputFile).mkString ==> "someOutput"

        verify(serializer).apply(
          locale = "de",
          mappings = Map(
            "test.string" -> "sss",
            "test.map.a" -> "aaa",
            "test.map.b" -> "bbb",
            "test.map.c" -> "ccc"
          )
        )
        ()
      }
    }
  }

}
