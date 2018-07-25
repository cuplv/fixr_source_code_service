package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File
import java.nio.file.Files

import edu.colorado.plv.fixr.storage.{MethodKey, MemoryMap}

class TestParser extends FlatSpec with Matchers with BeforeAndAfter {

  val dstFile = new File("TestMainClass.java")
  val sourceCodeMap = new MemoryMap()

  "The source parser" should  "extract the main method" in {
    val keyMain = MethodKey("TestMainClass.java",5,"main(java.lang.String[])")
    val mainMethod = """public static void main(String[] args) {

  }"""
    sourceCodeMap.lookupMethod(keyMain) should be (Option(mainMethod))
  }

  "The source parser" should  "find a method of an inner class" in {
    val keyInner2 = MethodKey("TestMainClass.java",32,"innerClass2Method()")
    val inner2Method = """@Override
      public void innerClass2Method() {
        int i = 0;
        i = i + 1;
        return;
      }"""
    sourceCodeMap.lookupMethod(keyInner2) should be (Option(inner2Method))
  }

  "The source parser" should  "extract the constructor of an inner class" in {
    val keyConstructor = MethodKey("TestMainClass.java",48,"simple.TestMainClass$InnerClass2()")
    val constructorMethod = """public InnerClass2() {
    }"""
    sourceCodeMap.lookupMethod(keyConstructor) should be (Option(constructorMethod))
  }

  before {
    val url = getClass.getResource("/TestMainClass.java")
    val file = new File(url.toURI())

    Files.copy(file.toPath(), dstFile.toPath())

    sourceCodeMap.clear()
    ClassParser.parseClassFile(sourceCodeMap, dstFile.getPath())
  }

  after {
    Files.delete(dstFile.toPath())
  }
}
