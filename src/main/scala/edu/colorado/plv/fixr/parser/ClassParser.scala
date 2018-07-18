package edu.colorado.plv.fixr.parser

import spoon.Launcher
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager

import edu.colorado.plv.fixr.storage.SourceCodeMap


object ClassParser {

  /**
    * Parse a java file and extract its methods source code
    * (stored in sourceCodeMap)
   */
  def parseClassFile(sourceCodeMap : SourceCodeMap, inputFileName : String) : Unit = {
    lazy val launcher : Launcher = new Launcher()

    val args1 = Array("-i", inputFileName)

    launcher.setArgs(args1)
    launcher.buildModel()

    val factory : Factory = launcher.getFactory()
    val processingManager : ProcessingManager = new QueueProcessingManager(factory)

    val method_processor = new MethodProcessor(sourceCodeMap)
    processingManager.addProcessor(method_processor)

    val constructor_processor = new ConstructorProcessor(sourceCodeMap)
    processingManager.addProcessor(constructor_processor)

    processingManager.process(factory.Class().getAll())

    launcher.process()
  }
}
