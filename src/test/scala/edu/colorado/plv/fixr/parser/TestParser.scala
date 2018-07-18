package edu.colorado.plv.fixr.parser

import org.scalatest._
import java.io.File

class TestParser extends FlatSpec with Matchers {

  val url = getClass.getResource("/TestMainClass.java")
  val file = new File(url.toURI())

  "The text extractor" should  "extract the main method" in {
    val res = SourceExtractor.extractText(file, 49, 93)
    val resString = """public static void main(String[] args) {

  }"""
    res should be (Option(resString))
  }
}
