package edu.colorado.plv.fixr.parser

import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.{CtConstructor, CtExecutable, CtMethod}
import spoon.reflect.factory.Factory
import edu.colorado.plv.fixr.Logger
import edu.colorado.plv.fixr.storage.{FileInfo, MethodKey, RepoFileInfo, SourceCodeMap}


class MethodProcessor(sourceCodeMap : SourceCodeMap,
  fileInfo : FileInfo, factory : Factory)
    extends AbstractProcessor[CtMethod[_]] {

  /**
    * Method called to process a single CtMethod node in the AST
    */
  def process(method_decl : CtMethod[_]) : Unit = {
    CtExecutableProcessor.process(sourceCodeMap,
      method_decl, fileInfo, factory)
  }
}

class ConstructorProcessor(sourceCodeMap : SourceCodeMap,
  fileInfo : FileInfo, factory : Factory)
    extends AbstractProcessor[CtConstructor[_]] {

  /**
    * Method called to process a single CtConstructor node in the AST
    */
  def process(method_decl : CtConstructor[_]) : Unit = {
    CtExecutableProcessor.process(sourceCodeMap,
      method_decl, fileInfo, factory)
  }
}

object CtExecutableProcessor {

  /**
    * Helper method used to process a CtExecutable node.
    */
  def process(sourceCodeMap : SourceCodeMap,
    executable_decl : CtExecutable[_], fileInfo : FileInfo,
    factory : Factory) = {
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
      val env = factory.getEnvironment()

      val methodTextOption = SourceExtractor.extractTextString(
        fileInfo.fileContent,
        sourceStart, sourceEnd)

      methodTextOption match {
        case Some(methodText) => {
          Logger.debug(s"Insert method: $simpleName\n" +
            s"\tFrom file: $fileName\n" +
            s"\tStart line: $startLine\n" +
            s"\tSource start: $sourceStart\n" +
            s"\tSource end: $sourceEnd"
          )
          fileInfo match{
            case fileInfo:RepoFileInfo => {
              sourceCodeMap.insertMethod(MethodKey(fileInfo.repoUrl,
                fileInfo.commitId,
                fileName, startLine, simpleName),
                methodText)
            }
            case _ => // Don't cache files not associated with repo
          }

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
