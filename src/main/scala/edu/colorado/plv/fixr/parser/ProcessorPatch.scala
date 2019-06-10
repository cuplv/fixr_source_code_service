package edu.colorado.plv.fixr.parser

import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.{CtConstructor, CtMethod, CtExecutable}
import spoon.reflect.cu.SourcePosition

import edu.colorado.plv.fixr.Logger
import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap}


class MethodProcessorPatch(gitHubUrl : String, sourceCodeMap : SourceCodeMap)
    extends AbstractProcessor[CtMethod[_]] {

  /**
    * Method called to process a single CtMethod node in the AST
    */
  def process(method_decl : CtMethod[_]) : Unit = {
    CtExecutableProcessor.process(gitHubUrl, sourceCodeMap, method_decl)
  }
}

class ConstructorProcessorPatch(gitHubUrl: String, sourceCodeMap : SourceCodeMap)
    extends AbstractProcessor[CtConstructor[_]] {

  /**
    * Method called to process a single CtConstructor node in the AST
    */
  def process(method_decl : CtConstructor[_]) : Unit = {
    CtExecutableProcessor.process(gitHubUrl, sourceCodeMap, method_decl)
  }
}

object CtExecutableProcessorPatch {

  /**
    * Helper method used to process a CtExecutable node.
    */
  def process(gitHubUrl : String, sourceCodeMap : SourceCodeMap,
    executable_decl : CtExecutable[_]) = {
    var simpleName = executable_decl.getSimpleName
    val signature = executable_decl.getSignature
    val sourcePosition = executable_decl.getPosition
    val startLine = sourcePosition.getLine

    Logger.debug(s"Processing method $simpleName ($signature) at line $startLine...")

    if (startLine > 0) {
      val methodFile = sourcePosition.getFile
      val fileName = methodFile.getName
      val sourceStart = sourcePosition.getSourceStart
      val sourceEnd = sourcePosition.getSourceEnd

      val methodTextOption = SourceExtractor.extractText(methodFile, sourceStart,
        sourceEnd)

      methodTextOption match {
        case Some(methodText) => {
          Logger.debug(s"Insert method: $simpleName\n" +
            s"\tFrom file: $fileName\n" +
            s"\tStart line: $startLine\n" +
            s"\tSource start: $sourceStart\n" +
            s"\tSource end: $sourceEnd"
          )

          sourceCodeMap.insertMethod(new MethodKey(gitHubUrl,fileName, startLine, simpleName),
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
  }
}
