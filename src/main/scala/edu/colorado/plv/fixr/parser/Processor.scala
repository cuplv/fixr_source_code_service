package edu.colorado.plv.fixr.parser

import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtPackage
import spoon.reflect.declaration.CtAnonymousExecutable

import edu.colorado.plv.fixr.Logger


class MethodProcessor extends AbstractProcessor[CtMethod[_]] {



  def process(method_decl : CtMethod[_]) : Unit = {
    val methodClass = method_decl.getParent(classOf[CtClass[_]])
    val classPackage = methodClass.getPackage()


    // println("Method: " + method_decl.getSignature())
    // println("Containing class " + methodClass.getSimpleName())
    // if (classPackage != null) {
    //   println("Package Name " + classPackage.getQualifiedName + "\n")
    // } else {
    //   println("Package is null\n")

    //   if (methodClass.isInstanceOf[CtAnonymousExecutable]) {
    //     println("Anonymous exec")
    //   }

    //   println(methodClass.isAnonymous())
    // }

    val sourcePosition = method_decl.getPosition
    val methodFile = sourcePosition.getFile
    val startLine = sourcePosition.getLine
    val startColumn = sourcePosition.getColumn
    val endLine = sourcePosition.getEndLine
    val endColumn = sourcePosition.getEndColumn
    val fileName = methodFile.getName

    val sourceStart = sourcePosition.getSourceStart
    val sourceEnd = sourcePosition.getSourceEnd


    Logger.debug(s"Method file: $fileName\n" +
      s"\tStart line: $startLine\n" +
      s"\tStart column: $startColumn\n" +
      s"\tEnd line: $endLine\n" +
      s"\tEnd column: $endColumn\n" +
      s"\tSource start: $sourceStart\n" +
      s"\tSource end: $sourceEnd"
    )
  }
}

object MethodProcessor {
}
