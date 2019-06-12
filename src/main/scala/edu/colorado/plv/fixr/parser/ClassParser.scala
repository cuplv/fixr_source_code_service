package edu.colorado.plv.fixr.parser

import spoon.Launcher
import spoon.compiler.Environment
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager
import spoon.reflect.declaration.{
  CtExecutable, CtMethod, CtConstructor
}
import spoon.reflect.code.{CtInvocation, CtComment, CtStatement}
import spoon.reflect.visitor.filter.{TypeFilter, AbstractFilter}
import spoon.reflect.visitor.DefaultJavaPrettyPrinter
//import spoon.reflect.visitor.SniperJavaPrettyPrinter
import spoon.support.compiler.{VirtualFile}

import edu.colorado.plv.fixr.storage.{SourceCodeMap, MethodKey, FileInfo}
import edu.colorado.plv.fixr.Logger

import scala.collection.JavaConversions._
import scala.math.abs

object ClassParser {

  /**
    * Parse a java file and extract its methods source code
    * (stored in sourceCodeMap)
   */
  def parseClassFile(sourceCodeMap : SourceCodeMap,
    fileInfo : FileInfo) : Unit = {
    lazy val launcher : Launcher = new Launcher()

    Logger.info(s"Parsing ${fileInfo.declaringFile}")

    val virtualFile = new VirtualFile(fileInfo.fileContent,
      fileInfo.declaringFile)
    launcher.addInputResource(virtualFile)

    val env = launcher.getEnvironment()
    env.setNoClasspath(true)
    env.setShouldCompile(false)
    env.setCommentEnabled(true)
    env.setPreserveLineNumbers(true)
    launcher.buildModel()

    val factory : Factory = launcher.getFactory()

    val processingManager : ProcessingManager =
      new QueueProcessingManager(factory)

    val method_processor = new MethodProcessor(fileInfo.repoUrl,
      sourceCodeMap, fileInfo, factory)
    processingManager.addProcessor(method_processor)
    val constructor_processor = new ConstructorProcessor(fileInfo.repoUrl,
      sourceCodeMap, fileInfo, factory)
    processingManager.addProcessor(constructor_processor)

    val elements =
      factory.getModel.getElements(new TypeFilter(classOf[CtExecutable[_]]))

    processingManager.process(elements)

    launcher.process()
  }

  def parseAndPatchClassFile(methodKey : MethodKey,
    fileInfo : FileInfo,
    diffsToApply : Map[Int, List[CommentDiff]]) : Option[String] = {

    Logger.info(s"Parsing ${fileInfo.declaringFile}")

    lazy val launcher : Launcher = new Launcher()
    val virtualFile = new VirtualFile(fileInfo.fileContent,
      fileInfo.declaringFile)
    launcher.addInputResource(virtualFile)

    val env = launcher.getEnvironment()
    env.setNoClasspath(true)
    env.setShouldCompile(false)
    env.setCommentEnabled(true)
    env.setPreserveLineNumbers(true)
    launcher.buildModel()

    val factory : Factory = launcher.getFactory()

    // Get the list of method declaration to be changed
    val methodDecls =
      factory.getModel.getElements(
        new AbstractFilter(classOf[CtExecutable[_]]) {
          override def matches(executable : CtExecutable[_]) : Boolean = {
            if (executable.isInstanceOf[CtMethod[_]] ||
              executable.isInstanceOf[CtConstructor[_]]) {
              val fileName = executable.getPosition.getFile.getName
              val startLine = executable.getPosition.getLine

              methodKey.declaringFile == fileName &&
              math.abs(methodKey.startLine - startLine) < 5 &&
              methodKey.methodName == executable.getSimpleName
            } else false
          }})

    // Method invocations that have a match with a diff entry
    val methodDeclRes = methodDecls.toList match {
      case x :: xs => Some(x)
      case Nil => None
    }

    methodDeclRes match {
      case Some(methodDecl) => {
        // Get all the invocations in the first method we found.
        // We should not find more than 1 method declaration with the
        // same name and in the 5 line interval
        val invocations = methodDecl.getElements(
          new AbstractFilter(classOf[CtInvocation[_]]) {
            override def matches (invocation : CtInvocation[_]) : Boolean = {
              diffsToApply.contains(invocation.getPosition.getLine)
            }
          })
        invocations.toList

        // Insert the comments
        invocations.forEach(invocation => {
          val line = invocation.getPosition.getLine
          val diffsAtLineRes = diffsToApply.get(line)

          diffsAtLineRes match {
            case Some(diffsAtLine) => {
              diffsAtLine.forEach(commentDiff => {
                val commentType =
                  if (commentDiff.isMultiLine)
                    CtComment.CommentType.INLINE
                  else
                    CtComment.CommentType.BLOCK

                val comment = factory.Code().createComment(commentDiff.diffText,
                  commentType)

                // Add the comment to the first statement parent
                val parent = invocation.getParent(classOf[CtStatement])
                parent.addComment(comment)
              })
            }
            case None => {
              Logger.debug("Not found diff at line ${line}")
              ()
            }
          }
        })

        // Return the modified comment
        Some(ClassParser.printMethodDecl(env, methodDecl))
      }
      case None => None
    }
  }

  def printMethodDecl(env : Environment,
    methodDecl : CtExecutable[_]) : String = {
    env.setCommentEnabled(true)
    val prettyPrinter = new DefaultJavaPrettyPrinter(env)

    val isPreserveLineNumbers = env.isPreserveLineNumbers()
    val isCommentsEnabled = env.isCommentsEnabled()
    env.setPreserveLineNumbers(false)
    env.setCommentEnabled(true)
    prettyPrinter.scan(methodDecl)
    val printed = prettyPrinter.toString()
    env.setPreserveLineNumbers(isPreserveLineNumbers)
    env.setCommentEnabled(isCommentsEnabled)
    printed
  }

  def convertStreamToString(is : java.io.InputStream) : String = {
    val scanner = new java.util.Scanner(is).useDelimiter("\\A")
    if (scanner.hasNext())
      scanner.next()
    else
      ""
  }
}


