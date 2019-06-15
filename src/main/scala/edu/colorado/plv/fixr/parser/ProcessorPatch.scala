package edu.colorado.plv.fixr.parser

import spoon.processing.AbstractProcessor
import spoon.reflect.factory.Factory
import spoon.reflect.declaration.{CtConstructor, CtMethod, CtExecutable}
import spoon.reflect.code.{CtInvocation, CtComment, CtStatement}
import spoon.reflect.visitor.filter.TypeFilter
import spoon.reflect.cu.SourcePosition

import spoon.reflect.visitor.DefaultJavaPrettyPrinter
//import spoon.reflect.visitor.SniperJavaPrettyPrinter

import edu.colorado.plv.fixr.Logger
import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap}
import edu.colorado.plv.fixr.service.SrcFetcherActor.{DiffEntry, SourceDiff}

import scala.collection.JavaConversions._

class MethodProcessorPatch(gitHubUrl : String,
  sourceCodeMap : SourceCodeMap, diffsToApply : List[SourceDiff])
    extends AbstractProcessor[CtMethod[_]] {

  /**
    * Method called to process a single CtMethod node in the AST
    */
  def process(method_decl : CtMethod[_]) : Unit = {
    CtCollectPatchList.process(gitHubUrl, sourceCodeMap, method_decl,
      diffsToApply, getFactory())
  }
}

class ConstructorProcessorPatch(gitHubUrl: String,
  sourceCodeMap : SourceCodeMap, diffsToApply : List[SourceDiff])
    extends AbstractProcessor[CtConstructor[_]] {

  /**
    * Method called to process a single CtConstructor node in the AST
    */
  def process(method_decl : CtConstructor[_]) : Unit = {

    CtCollectPatchList.process(gitHubUrl, sourceCodeMap, method_decl,
      diffsToApply, getFactory())
  }
}

object CtCollectPatchList {

  /**
    * Helper method used to process a CtExecutable node.
    */
  def process(gitHubUrl : String, sourceCodeMap : SourceCodeMap,
    executable_decl : CtExecutable[_], diffsToApply : List[SourceDiff],
    factory : Factory) = {

    val typeFilter = new TypeFilter(classOf[CtInvocation[_]])
    val methodInvocations = executable_decl.getElements(typeFilter)

    // Process the method invocations
    methodInvocations.toList.forEach{
      methodInvocation => {
        val sourcePosition = methodInvocation.getPosition
        val startLine = sourcePosition.getLine
        var ctReference = methodInvocation.getExecutable()
        var simpleName = ctReference.getSimpleName

        if (simpleName != "otherMethodToCall") {

        } else {


        Logger.debug(s"Processing invocation $simpleName at line $startLine...")

        val lineComment = factory.Code().createComment("cavallo goloso",
          CtComment.CommentType.INLINE);

        val parent = methodInvocation.getParent(classOf[CtStatement])
        parent.addComment(lineComment)

        //val parent = methodInvocation.getParent(classOf[CtStatement])
        //methodInvocation.addComment(lineComment)
        }

    }}

    val env = factory.getEnvironment()
    // env.setAutoImports(true);
    //env.setCommentEnabled(true)
    val prettyPrinter = new DefaultJavaPrettyPrinter(env)
//    val prettyPrinter = new SniperJavaPrettyPrinter(env)

    env.setPreserveLineNumbers(false)
    env.setCommentEnabled(true)
    prettyPrinter.scan(executable_decl)
    val printed = prettyPrinter.toString()
    env.setPreserveLineNumbers(true)
    Logger.debug(s"Result is ${printed}")



    // var simpleName = executable_decl.getSimpleName
    // val signature = executable_decl.getSignature
    // val sourcePosition = executable_decl.getPosition
    // val startLine = sourcePosition.getLine

    // Logger.debug(s"Processing method $simpleName ($signature) at line $startLine...")

    // if (startLine > 0) {
    //   val methodFile = sourcePosition.getFile
    //   val fileName = methodFile.getName
    //   val sourceStart = sourcePosition.getSourceStart
    //   val sourceEnd = sourcePosition.getSourceEnd

    //   val methodTextOption = SourceExtractor.extractText(methodFile, sourceStart,
    //     sourceEnd)

    //   methodTextOption match {
    //     case Some(methodText) => {
    //       Logger.debug(s"Insert method: $simpleName\n" +
    //         s"\tFrom file: $fileName\n" +
    //         s"\tStart line: $startLine\n" +
    //         s"\tSource start: $sourceStart\n" +
    //         s"\tSource end: $sourceEnd"
    //       )

    //       sourceCodeMap.insertMethod(new MethodKey(gitHubUrl,fileName, startLine, simpleName),
    //         methodText)
    //     }
    //     case _ => {
    //       Logger.warn(s"Cannot extract text for method method: $simpleName\n" +
    //         s"\tFrom file: $fileName\n" +
    //         s"\tStart line: $startLine\n" +
    //         s"\tSource start: $sourceStart\n" +
    //         s"\tSource end: $sourceEnd"
    //       )
    //     }
    //   }
    // } else {
    //   // Element not declared in this file, like class constructors - do nothing
    //   ()
    // }
  }
   
}
