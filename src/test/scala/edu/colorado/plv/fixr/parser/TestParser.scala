package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File
import java.nio.file.Files

import edu.colorado.plv.fixr.storage.{MethodKey, MemoryMap}

class TestParser extends FlatSpec with Matchers with BeforeAndAfter {

  val githubUrl = "https://github.com/cuplv/fixr_source_code_service.git"
  val dstFile = new File("TestMainClass.java")
  val sourceCodeMap = new MemoryMap()

  "The source parser" should  "extract the main method" in {
    val keyMain = MethodKey(githubUrl,"TestMainClass.java", 5,"main")
    val mainMethod = """public static void main(String[] args) {

  }
"""
    sourceCodeMap.lookupMethod(keyMain) should be (Option(Set(mainMethod)))
    sourceCodeMap.lookupClosestMethod(keyMain) should be (Option( (5,Set(mainMethod))) )

    val keyMainApprox = MethodKey(githubUrl,"TestMainClass.java", 2,"main")
    sourceCodeMap.lookupClosestMethod(keyMainApprox) should be (Option( (5,Set(mainMethod))) )
  }


  "The source parser" should  "find a method of an inner class" in {
    val keyInner2 = MethodKey(githubUrl,"TestMainClass.java",
      31,"innerClass2Method") // innerClass2Method()
    val inner2Method = """@Override
      public void innerClass2Method() {
        int i = 0;
        i = i + 1;
        return;
      }
"""
    sourceCodeMap.lookupMethod(keyInner2) should be (Option(Set(inner2Method)))
  }

  "The source parser" should  "extract the constructor of an inner class" in {
    val keyConstructor = MethodKey(githubUrl,"TestMainClass.java",
      48,"<init>") // "simple.TestMainClass$InnerClass2()"
    val constructorMethod = """public InnerClass2() {
    }
"""
    sourceCodeMap.lookupMethod(keyConstructor) should be (Option(Set(constructorMethod)))
  }

  before {
    val url = getClass.getResource("/TestMainClass.java")
    val file = new File(url.toURI())

    Files.copy(file.toPath(), dstFile.toPath())

    sourceCodeMap.clear()
    // ClassParser.parseClassFile(githubUrl, sourceCodeMap, dstFile.getPath())

    JdtClassParser.parseClassFile(githubUrl, sourceCodeMap, dstFile.getPath())



  }

  after {
    Files.delete(dstFile.toPath())
  }
}
