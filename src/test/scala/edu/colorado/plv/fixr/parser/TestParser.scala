package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File
import java.nio.file.Files

import edu.colorado.plv.fixr.storage.{MethodKey, MemoryMap, FileInfo}

class TestParser extends FlatSpec with Matchers with BeforeAndAfter {

  val githubUrl = "https://github.com/cuplv/fixr_source_code_service.git"
  val sourceName = "TestMainClass.java"
  val dstFile = new File(sourceName)
  val sourceCodeMap = new MemoryMap()

  "The source parser" should  "extract the main method" in {
    val keyMain = MethodKey(githubUrl,sourceName, 5,"main")
    val mainMethod = """public static void main(String[] args) {

  }"""
    sourceCodeMap.lookupMethod(keyMain) should be (Option(Set(mainMethod)))
    sourceCodeMap.lookupClosestMethod(keyMain) should be (Option( (5,Set(mainMethod))) )

    val keyMainApprox = MethodKey(githubUrl,sourceName, 2,"main")
    sourceCodeMap.lookupClosestMethod(keyMainApprox) should be (Option( (5,Set(mainMethod))) )
  }


  "The source parser" should  "find a method of an inner class" in {
    val keyInner2 = MethodKey(githubUrl,sourceName,
      32,"innerClass2Method") // innerClass2Method()
    val inner2Method = """@Override
      public void innerClass2Method() {
        int i = 0;
        i = i + 1;
        return;
      }"""
    val lookUp = sourceCodeMap.lookupClosestMethod(keyInner2)

    lookUp match {
      case Some( (line, code) ) => {
        line should equal (32+-4)
        code should be (Set(inner2Method))
      }
      case None => lookUp should not be None
    }

    //sourceCodeMap.lookupClosestMethod(keyInner2) should be (Option( (32, Set(inner2Method))))
  }

  "The source parser" should  "extract the constructor of an inner class" in {
    val keyConstructor = MethodKey(githubUrl,sourceName,
      48,"<init>") // "simple.TestMainClass$InnerClass2()"
    val constructorMethod = """public InnerClass2() {
    }"""
    sourceCodeMap.lookupMethod(keyConstructor) should be (Option(Set(constructorMethod)))
  }

  before {
    val url = getClass.getResource("/TestMainClass.java")
    val file = new File(url.toURI())

    if (! Files.exists(dstFile.toPath()))
        Files.copy(file.toPath(), dstFile.toPath())

    sourceCodeMap.clear()
    sourceCodeMap.clearFile()

    val fileContent = new String(Files.readAllBytes(dstFile.toPath()))
    val fileInfo = FileInfo(githubUrl,
      sourceName,
      dstFile.toPath().toString(),
      fileContent)

    ClassParser.parseClassFile(githubUrl, sourceCodeMap, dstFile.getPath(), fileInfo)

    // JdtClassParser.parseClassFile(githubUrl, sourceCodeMap, dstFile.getPath())



  }

  after {
  }
}
