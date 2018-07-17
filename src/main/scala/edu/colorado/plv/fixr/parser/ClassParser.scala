package edu.colorado.plv.fixr.parser

import spoon.Launcher
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager



class ClassParser {
  lazy val launcher : Launcher = new Launcher()
}

object ClassParser {
  def parseClassFile(parser : ClassParser) : ClassParser = {
    val args1 = Array("-i", "/Users/sergiomover/works/projects/muse/repos/fixr_source_code_service/src/test/resources/TestMainClass.java", "-o", "/tmp/cavallo")

    parser.launcher.setArgs(args1)
    parser.launcher.buildModel()

    val factory : Factory = parser.launcher.getFactory()
    val processingManager : ProcessingManager = new QueueProcessingManager(factory)

    val method_processor = new MethodProcessor()
    processingManager.addProcessor(method_processor)

    processingManager.process(factory.Class().getAll())

    parser.launcher.process()

    parser
  }
}
