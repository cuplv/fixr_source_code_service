package edu.colorado.plv.fixr.parser

import spoon.Launcher
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager
import spoon.reflect.declaration.{CtExecutable, CtMethod}
import spoon.reflect.visitor.filter.{TypeFilter,
  AbstractFilter}

import scala.math.abs

import edu.colorado.plv.fixr.storage.{SourceCodeMap, MethodKey}
import edu.colorado.plv.fixr.Logger
import edu.colorado.plv.fixr.service.SrcFetcherActor.{SourceDiff}


object ClassParser {

  /**
    * Parse a java file and extract its methods source code
    * (stored in sourceCodeMap)
   */
  def parseClassFile(gitHubUrl : String,
    sourceCodeMap : SourceCodeMap,
    inputFileName : String) : Unit = {
    lazy val launcher : Launcher = new Launcher()

    Logger.info(s"Parsing $inputFileName")

    val args1 = Array("-i", inputFileName)

    launcher.setArgs(args1)

    val env = launcher.getEnvironment()
    env.setNoClasspath(true)
    env.setShouldCompile(false)
    env.setCommentEnabled(true)
    env.setPreserveLineNumbers(true)
    launcher.buildModel()

    val factory : Factory = launcher.getFactory()

    val processingManager : ProcessingManager =
      new QueueProcessingManager(factory)

    val method_processor = new MethodProcessor(gitHubUrl, sourceCodeMap)
    processingManager.addProcessor(method_processor)

    val constructor_processor = new ConstructorProcessor(gitHubUrl, sourceCodeMap)
    processingManager.addProcessor(constructor_processor)

    val elements =
      factory.getModel.getElements(new TypeFilter(classOf[CtExecutable[_]]))

    processingManager.process(elements)

    launcher.process()
  }

  def parseAndPatchClassFile(gitHubUrl : String,
    sourceCodeMap : SourceCodeMap,
    inputFileName : String,
    methodKey : MethodKey,
    diffsToApply : List[SourceDiff]) : Option[String] = {
    lazy val launcher : Launcher = new Launcher()

    Logger.info(s"Parsing $inputFileName")

    val args1 = Array("-i", inputFileName)

    launcher.setArgs(args1)

    val env = launcher.getEnvironment()
    env.setNoClasspath(true)
    env.setShouldCompile(false)
    env.setCommentEnabled(true)
    env.setPreserveLineNumbers(true)
    launcher.buildModel()

    val factory : Factory = launcher.getFactory()

    val processingManager : ProcessingManager =
      new QueueProcessingManager(factory)

    val method_processor = new MethodProcessorPatch(gitHubUrl, sourceCodeMap,
      diffsToApply)
    processingManager.addProcessor(method_processor)

    val constructor_processor =
      new ConstructorProcessorPatch(gitHubUrl, sourceCodeMap, diffsToApply)
    processingManager.addProcessor(constructor_processor)

    // var filter = new TypeFilter(classOf[CtExecutable[_]])
    var filter = new AbstractFilter(classOf[CtMethod[_]]) {
      override def matches(executable_decl : CtMethod[_]) : Boolean = {
        val simpleName = executable_decl.getSimpleName
        val signature = executable_decl.getSignature
        val sourcePosition = executable_decl.getPosition
        val startLine = sourcePosition.getLine
        val methodFile = sourcePosition.getFile

        Logger.debug(s"Checking match for ${simpleName}...")

        methodFile match {
          case null => false
          case _ => {
            val fileName = methodFile.getName

            val res = (methodKey.declaringFile == fileName &&
              math.abs(methodKey.startLine - startLine) < 5 &&
              methodKey.methodName == simpleName)

            Logger.debug(s"Match? ${res}")

            res
          }}


      }
    }
    val elements = factory.getModel.getElements(filter)

    processingManager.process(elements)

    launcher.process()

    return Some("abc")
  }
}
