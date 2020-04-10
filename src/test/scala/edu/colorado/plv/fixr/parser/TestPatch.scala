package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import edu.colorado.plv.fixr.storage.{FileInfo, MemoryMap, MethodKey, RepoFileInfo}


class TestPatch extends FlatSpec with Matchers with BeforeAndAfter {

  val githubUrl = "https://github.com/cuplv/fixr_source_code_service.git"
  val sourceName = "TestMainClass.java"
  val dstFile = new File(sourceName)
  val commitId = "commit_id"

  val sourceCodeMap = new MemoryMap()
  var fileInfo : Option[FileInfo] = None


  "The patcher" should  "patch the callMethod" in {
    val methodKey = MethodKey(githubUrl, commitId,sourceName,
      55, "callMethodA")

    val commentDiffs = Map (
      56 -> List(
        CommentDiff(1, 56, """[1] After this method call (otherMethodCall(3))
You should invoke the following methods
intMethod(3)""", true, true, true)),
        57 -> List(
          CommentDiff(1, 57,
        "[1] The change should end here (before calling the method otherMethodCall(4))",
        false, false, false),
        ))

    val patchCode = """public void callMethodA() {
    /* [1] After this method call (otherMethodCall(3))
    You should invoke the following methods
    intMethod(3)
     */
    otherMethodToCall(3);
    // [1] The change should end here (before calling the method otherMethodCall(4))
    otherMethodToCall(4);
}"""


    val res = fileInfo match {
      case Some(f) => {
        ClassParser.parseAndPatchClassFile(methodKey.localMethodKey, f, commentDiffs)
      }
      case None => None
    }

    res should be (Some(patchCode))
  }

  before {
    val url = getClass.getResource("/" + sourceName)
    val file = new File(url.toURI())

    if (! Files.exists(dstFile.toPath()))
        Files.copy(file.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    sourceCodeMap.clear()

    val fileContent = new String(Files.readAllBytes(dstFile.toPath()))
    fileInfo = Some(RepoFileInfo(githubUrl, commitId,
      sourceName,
      dstFile.toPath().toString(),
      fileContent))
  }

  after {
  }

  def callPatch() = {
  }
}
