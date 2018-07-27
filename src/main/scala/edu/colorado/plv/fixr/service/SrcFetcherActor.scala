package edu.colorado.plv.fixr.service


import edu.colorado.plv.fixr.{SrcFinder, Logger}
import edu.colorado.plv.fixr.storage.{MethodKey,MemoryMap}

import akka.actor.{Actor, ActorLogging, Props}

object SrcFetcherActor {
  final case class FindMethodSrc(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodSignature : String)
  final case class MethodSrcReply(src : String, errorDesc : String)

  def props : Props = Props[SrcFetcherActor]
}

class SrcFetcherActor extends Actor with ActorLogging {
  import SrcFetcherActor._

  val finder = new SrcFinder(new MemoryMap())

  def receive : Receive = {
    case FindMethodSrc(githubUrl, commitId,
      declaringFile, methodLine, methodSignature) => {

      Logger.debug("Receive request:" +
        s" - githubUrl = $githubUrl" +
        s" - commitId = $commitId" +
        s" - declaringFile = $declaringFile" +
        s" - methodLine = $methodLine" +
        s" - methodSignature = $methodSignature")

      validateData(githubUrl, commitId, declaringFile,
        methodLine, methodSignature) match {
        case Some(element) => sender() ! element
        case None => {
          val methodKey = MethodKey(declaringFile,
            methodLine,
            methodSignature)

          finder.lookupMethod(githubUrl,
            commitId,
            methodKey) match {
            case Some(sourceCode) =>
              sender() ! MethodSrcReply(sourceCode, "")
            case none =>
              sender() ! MethodSrcReply("", "Cannot find the source code")
          }
        }
      }
    }
  }

  private def validateData(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodSignature : String) : Option[MethodSrcReply] = {
      githubUrl match {
        case "" => Some(MethodSrcReply("", "Empty github url"))
        case _ =>
          commitId match {
            case "" => Some(MethodSrcReply("", "Empty commit id"))
            case _ =>
              declaringFile match {
                case "" => Some(MethodSrcReply("", "Empty declaring file"))
                case _ =>
                  methodSignature match {
                    case "" => Some(MethodSrcReply("", "Empty signature"))
                    case _ => if (methodLine <= 0) 
                      Some(MethodSrcReply("", "Negative method line"))
                    else
                      None
                  }
              }
          }
      }
  }
}
