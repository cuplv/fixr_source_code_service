package edu.colorado.plv.fixr.service


import edu.colorado.plv.fixr.{SrcFinder, Logger}
import edu.colorado.plv.fixr.storage.{MethodKey,MemoryMap}

import akka.actor.{Actor, ActorLogging, Props}

object SrcFetcherActor {
  final case class FindMethodSrc(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodName : String)
  final case class MethodSrcReply(res : Set[String], errorDesc : String)

  def props : Props = Props[SrcFetcherActor]
}

class SrcFetcherActor extends Actor with ActorLogging {
  import SrcFetcherActor._

  val finder = new SrcFinder(new MemoryMap())

  def receive : Receive = {
    case FindMethodSrc(githubUrl, commitId,
      declaringFile, methodLine, methodName) => {

      Logger.debug("Receive request:" +
        s" - githubUrl = $githubUrl" +
        s" - commitId = $commitId" +
        s" - declaringFile = $declaringFile" +
        s" - methodLine = $methodLine" +
        s" - methodName = $methodName")

      validateData(githubUrl, commitId, declaringFile,
        methodLine, methodName) match {
        case Some(element) => sender() ! element
        case None => {
          val methodKey = MethodKey(githubUrl,
            declaringFile,
            methodLine,
            methodName)

          finder.lookupMethod(githubUrl, commitId,
            methodKey) match {
            case Some(sourceCodeList) =>
              sender() ! MethodSrcReply(sourceCodeList, "")
            case none =>
              sender() ! MethodSrcReply(Set(),
                "Cannot find the source code")
          }
        }
      }
    }
  }

  private def validateData(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodName : String) : Option[MethodSrcReply] = {
      githubUrl match {
        case "" => Some(MethodSrcReply(Set(), "Empty github url"))
        case _ =>
          commitId match {
            case "" => Some(MethodSrcReply(Set(), "Empty commit id"))
            case _ =>
              declaringFile match {
                case "" => Some(MethodSrcReply(Set(), "Empty declaring file"))
                case _ =>
                  methodName match {
                    case "" => Some(MethodSrcReply(Set(), "Empty method name"))
                    case _ => if (methodLine <= 0)
                      Some(MethodSrcReply(Set(), "Negative method line"))
                    else
                      None
                  }
              }
          }
      }
  }
}
