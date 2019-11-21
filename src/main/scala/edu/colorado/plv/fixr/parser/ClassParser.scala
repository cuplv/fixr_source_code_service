package edu.colorado.plv.fixr.parser

import edu.colorado.plv.fixr.storage.LocalMethodKey
import spoon.Launcher
import spoon.compiler.Environment
import spoon.reflect.factory.Factory
import spoon.processing.ProcessingManager
import spoon.support.QueueProcessingManager
import spoon.reflect.declaration.{CtConstructor, CtElement, CtExecutable, CtMethod}
import spoon.reflect.path.CtRole
import spoon.reflect.code.{CtComment, CtInvocation}
import spoon.reflect.visitor.filter.{AbstractFilter, TypeFilter}
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

    val method_processor = new MethodProcessor(sourceCodeMap, fileInfo, factory)
    processingManager.addProcessor(method_processor)
    val constructor_processor = new ConstructorProcessor(sourceCodeMap, fileInfo, factory)
    processingManager.addProcessor(constructor_processor)

    val elements =
      factory.getModel.getElements(new TypeFilter(classOf[CtExecutable[_]]))

    processingManager.process(elements)

    launcher.process()
  }


  def parseAndPatchClassFile(methodKey : LocalMethodKey,
    fileInfo : FileInfo,
    diffsToApply : Map[Int, List[CommentDiff]]) : Option[String] = {

    Logger.debug(s"Parsing and patching ${fileInfo.declaringFile}")

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
              val fileName = fileInfo.declaringFile
              val startLine = executable.getPosition.getLine

              // Logger.debug(s"${methodKey.declaringFile} == ${fileName} \n" +
              //   s"${methodKey.startLine} - ${startLine} < 5 \n" +
              //   s"${methodKey.methodName} == ${executable.getSimpleName}")
              // Logger.debug(s"Checking ${methodKey.methodName}")

              methodKey.declaringFile == fileName &&
              math.abs(methodKey.startLine - startLine) < 5 &&
              methodKey.methodName == executable.getSimpleName
            } else false
          }})

    // Method invocations that have a match with a diff entry
    val methodDeclRes = methodDecls.toList match {
      case x :: xs => Some(x)
      case Nil => {
        Logger.debug(s"Did not find the method ${methodKey.methodName}")
        None
      }
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

        // Insert the comments
        invocations.forEach(invocation => {
          val line = invocation.getPosition.getLine
          val diffsAtLineRes = diffsToApply.get(line)

          diffsAtLineRes match {
            case Some(diffsAtLine) => {
              diffsAtLine.forEach( commentDiff =>
                addComment(factory, commentDiff,
                  invocation, methodDecl)
              )
            }
            case None => {
              Logger.debug("Not found diff at line ${line}")
              ()
            }
          }
        })

        // Insert comments at the top or bottom of the method
        val rootsAndTails = diffsToApply.get(0)
        rootsAndTails match {
          case Some(diffsAtLine) => {
            diffsAtLine.forEach( commentDiff => {
              Logger.debug(s"O-line diff entry: ${commentDiff}")

              if (commentDiff.isMultiLine) {
                val body = methodDecl.getBody()
                val comment = getComment(factory, commentDiff)
                body.insertBegin(comment)
              } else {
                val body = methodDecl.getBody()
                val comment = getComment(factory, commentDiff)
                body.insertEnd(comment)
              }
            })
          }
          case None => None
        }

        // Return the string representation of the method
        Some(ClassParser.printMethodDecl(env, methodDecl))
      }
      case None => {
        None
      }
    }
  }

// use null -- dirty, but otherwise we get:
// java.lang.ClassCastException: spoon.support.reflect.code.CtInvocationImpl cannot be cast to scala.runtime.BoxedUnit

  def getComment(factory : Factory, commentDiff : CommentDiff) = {
    val commentType =
      if (commentDiff.isMultiLine)
        CtComment.CommentType.BLOCK
      else
        CtComment.CommentType.INLINE

    factory.Code().createComment(commentDiff.diffText, commentType)
  }

  def addComment(factory : Factory,
    commentDiff : CommentDiff,
    element : CtElement, // use null, otherwise we get a nasty exception
    topElement : CtElement) : Unit = {
    val comment = getComment(factory, commentDiff)

    // Add the comment to the first (real) statement
    if (element != null) {
      def findParent(statement : CtElement) : CtElement = {
        if (statement != null &&
          statement.getRoleInParent() != null &&
          statement.getRoleInParent() == CtRole.STATEMENT) {
          Logger.debug(s"${statement}")
          statement
        } else if (statement == null) {
          null
        } else {
          findParent(statement.getParent())
        }
      }

      val statement = findParent(element)

      // val parent = invocation.getParent(classOf[CtStatement])
      if (statement != null) {
        // Logger.debug(s"Adding comment ${comment} to ${statement}")
        statement.addComment(comment)
      } else {
        Logger.debug(s"Adding comment ${comment} to ${topElement}")
        topElement.addComment(comment)
      }
    } else {
      // Logger.debug(s"Adding comment ${comment} to ${topElement}")
      topElement.addComment(comment)
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


