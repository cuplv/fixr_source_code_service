package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File
import java.nio.file.{Files}

import edu.colorado.plv.fixr.storage.{MethodKey, MemoryMap}
import edu.colorado.plv.fixr.service.SrcFetcherActor.{SourceDiff, DiffEntry}

class TestPatch extends FlatSpec with Matchers with BeforeAndAfter {

  val githubUrl = "https://github.com/cuplv/fixr_source_code_service.git"
  val dstFile = new File("TestMainClass.java")
  val commitId = "commit_id"
  val sourceCodeMap = new MemoryMap()

  "The patcher" should  "patch the callMethod" in {
    val keyCallMethod = MethodKey(githubUrl,
      commitId,
      "TestMainClass.java",
      56,
      "callMethod")

    val diffsToApply =
      List(SourceDiff("+",
        DiffEntry(59, "callMethod", "i = otherMethodToCall"),
        List(DiffEntry(0, "exit", ""))
      ))

    val patchCode = """public void callMethod() {
    int i = 0;

    i = intMethod("cavallo");
  }"""


    // val patchRes = ClassParser.parseAndPatchClassFile(githubUrl,
    //   sourceCodeMap,
    //   dstFile.getPath(),
    //   keyCallMethod,
    //   diffsToApply)

    // patchRes match {
    //   case Some( code ) => {
    //     code should be (Some(patchCode))
    //   }
    //   case None => patchRes should not be None
    // }
  }

  before {
    val url = getClass.getResource("/TestMainClass.java")
    val file = new File(url.toURI())

    if (! Files.exists(dstFile.toPath()))
        Files.copy(file.toPath(), dstFile.toPath())
    sourceCodeMap.clear()
  }

  after {
  }
}
