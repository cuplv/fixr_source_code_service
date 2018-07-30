package edu.colorado.plv.fixr.parser

import spoon.Launcher
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager
import spoon.reflect.declaration.{CtExecutable}
import spoon.reflect.visitor.filter.TypeFilter

import edu.colorado.plv.fixr.storage.SourceCodeMap
import edu.colorado.plv.fixr.Logger

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
}
