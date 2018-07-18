package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File

class TestExtractor extends FlatSpec with Matchers {

  val url = getClass.getResource("/TestMainClass.java")
  val file = new File(url.toURI())

  "The text extractor" should  "extract the main method" in {
    val res = SourceExtractor.extractText(file, 49, 93)
    val resString = """public static void main(String[] args) {

  }"""
    res should be (Option(resString))
  }

  "The text extractor" should  "extract the innerclass method" in {
    val res = SourceExtractor.extractText(file, 387, 496)
    val resString = """@Override
      public void innerClassMethod() {
        int i = 0;
        i = i + 1;
        return;
      }"""
    res should be (Option(resString))
  }
}
