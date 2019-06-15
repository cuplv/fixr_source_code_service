/** DO NOT USE --- it's not up to date!
  */


package edu.colorado.plv.fixr.parser

import scala.io.Source
import java.io.{BufferedReader, FileReader}
import org.eclipse.jdt.core.dom._
import org.eclipse.jdt.core.compiler.IProblem

import java.io.File
import org.eclipse.jdt.core.dom.{ITypeBinding, ASTVisitor,
  Expression, ImportDeclaration}
import org.eclipse.jdt.core.dom._
import scala.collection.JavaConversions._

import edu.colorado.plv.fixr.storage.SourceCodeMap
import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap}
import edu.colorado.plv.fixr.Logger


class MyVisitor(sourceCodeMap : SourceCodeMap,
  gitHubUrl : String,
  inputFileName : String) extends ASTVisitor {
  var sourceLines : Array[String] = null

  override def visit(node : MethodDeclaration) : Boolean = {
    val cu : CompilationUnit = node.getRoot().asInstanceOf[CompilationUnit]

    var simpleName = if (! node.isConstructor) {
      node.getName().getIdentifier()
    } else {
      "<init>"
    }
    val f = new File(inputFileName)
    val fileName = f.getName()
    val signature = node.toString
    val sourceStart = node.getStartPosition
    val nodeLength = node.getLength
    val sourceEnd = nodeLength + node.getStartPosition
    val startLine = cu.getLineNumber(sourceStart - 1)

    if (startLine > 0) {

      val methodTextOption : Option[String] =
        SourceExtractor.extractText(f, sourceStart, sourceEnd)

      methodTextOption match {
        case Some(methodText) => {
          Logger.debug(s"Insert method: $simpleName\n" +
            s"\tFrom file: $fileName\n" +
            s"\tStart line: $startLine\n" +
            s"\tSource start: $sourceStart\n" +
            s"\tSource end: $sourceEnd"
          )

          sourceCodeMap.insertMethod(new MethodKey(gitHubUrl,
            "",
            fileName,
            startLine, simpleName),
            methodText)
        }
        case _ => {
          Logger.warn(s"Cannot extract text for method method: $simpleName\n" +
            s"\tFrom file: $fileName\n" +
            s"\tStart line: $startLine\n" +
            s"\tSource start: $sourceStart\n" +
            s"\tSource end: $sourceEnd"
          )
        }
      }
    } else {
      // Element not declared in this file, like class constructors - do nothing
      ()
    }

    return super.visit(node);
  }

}

object JdtClassParser {
  /**
    * Parse a java file and extract its methods source code
    * (stored in sourceCodeMap)
   */
  def parseClassFile(gitHubUrl : String,
    sourceCodeMap : SourceCodeMap,
    inputFileName : String) : Unit = {

    val astVisitor  = new MyVisitor(sourceCodeMap, gitHubUrl,
      inputFileName)
    val visitors    = List[MyVisitor](astVisitor)

    val file = new File(inputFileName)
    try {
      val data = ParseTools.readFile(inputFileName)

      val sourceLines = data.split("\n")
      visitors.foreach {(astVisitor) => astVisitor.sourceLines = sourceLines}
      val stdClasspath : Array[String] = Array(System.getProperty("java.class.path"))
      val encodings : Array[String] = Array("UTF-8")
      val sources : Array[String] = Array(".")

      val resParse =
        ParseTools.parse(
          data,
          inputFileName,
          visitors,
          stdClasspath,
          sources,
          encodings)

    } catch {
      case e : Exception =>
        Logger.debug("e.getMessage")
    }
  }
}



object ParseTools {
  val parser = ASTParser.newParser(AST.JLS3)

  def readFile(f: String): String = {
    val source = Source.fromFile(f)
    val str = source.mkString
    source.close()
    return str
  }

  @throws(classOf[ParseException])
  def parse[T<:ASTVisitor](javaSource: String, javaFileName : String,
    visitors: List[T]): Unit = {
    val sources : Array[String] = Array(".")
    val stdClasspath : String = System.getProperty("java.class.path")
    val classpath : Array[String] = Array(".", stdClasspath)
    val encodings : Array[String] = Array("UTF-8")
    parse(javaSource, javaFileName, visitors, classpath, sources, Array("UTF-8"))
  }

  @throws(classOf[ParseException])
  def parse[T<:ASTVisitor](javaSource: String,
    javaFileName : String,
    visitors: List[T],
    classpath : Array[String],
    sourcePaths : Array[String],
    encodings : Array[String]): Unit = {

    val strCharArray : Array[Char] = javaSource.toCharArray()
    assert(null != javaFileName)

    initParser(javaFileName, classpath, sourcePaths, encodings, ASTParser.K_COMPILATION_UNIT)

    parser.setSource(strCharArray);
    val ast : ASTNode = parser.createAST(null)
    val cu = ast.asInstanceOf[CompilationUnit]
    assert (cu != null)

    if (0 == cu.getLength()) {
      throw new ParseException("Exception parsing file",
        List(ParseProblem(-1, "Empty file", -1, -1, -1)))
    }
    else {
      val parseProblems : List[ParseProblem] = getParseErrors(cu)
      parseProblems.foreach { problem =>
        Logger.debug(problem.toString)
      }
    }

    visitors.foreach {cu.accept}
  }

  private def initParser(javaFileName : String,
    classpath : Array[String],
    sourcePaths : Array[String],
    encodings : Array[String],
    kind : Int) {
    assert(null != javaFileName)
    parser.setKind(kind);
    /* Ask the parser to resolve the bindings (e.g. types) */
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);
    parser.setUnitName(javaFileName);
    parser.setEnvironment(classpath, sourcePaths, encodings, true);
  }

  /* Visit the comments
   *  The line and block comments are not visited by the standard AST traversal of JDT.
   *  They are available iterating the comment list of the compilation unit.
   * */
  private def iterateOnComments[T<:ASTVisitor](commentsIter : java.util.Iterator[ASTNode], visitors : List[T]) : Unit = {
    if (commentsIter.hasNext()) {
      commentsIter.next() match {
        case comment if (! comment.isInstanceOf[Javadoc]) => {visitors.foreach {comment.accept}}
        case _ => ()
      }
      iterateOnComments(commentsIter, visitors)
    } else ()
  }

  /**
    * Returns a list of parse errors.
    * If the list is empty, there are no errors.
    */
  private def getParseErrors(cu : CompilationUnit) : List[ParseProblem] = {
    val problems = cu.getProblems();

    val res = problems.foldLeft (List[ParseProblem]()) { (res, problem) => {

      if (problem.isError()) {
        /* we only care about errors */
        ParseProblem(problem.getID(),
          problem.getMessage(),
          problem.getSourceLineNumber(),
          problem.getSourceStart(),
          problem.getSourceEnd()) :: res
      }
      else {
        res
      }
    }}

    res.reverse
  }
}

/**
  * Gets the useful information from the IProblem object.
  * At this level we only log errors.
  */
case class ParseProblem(problemId : Int,
  message : String,
  sourceLineNumber : Int,
  sourceStart : Int,
  sourceEnd : Int
)

case class ParseException(message: String = "",
  parseProblems : List[ParseProblem],
  cause: Throwable = null)
    extends Exception(message, cause)
