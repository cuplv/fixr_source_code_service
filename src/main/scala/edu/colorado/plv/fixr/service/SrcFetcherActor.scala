package edu.colorado.plv.fixr.service


import edu.colorado.plv.fixr.SrcFinder
import edu.colorado.plv.fixr.storage.{MethodKey,MemoryMap}

import akka.actor.{Actor, ActorLogging, Props}

object SrcFetcherActor {
  final case class FindMethodSrc(githubUrl : String,
    commit_id : String,
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
    case FindMethodSrc(githubUrl, commit_id,
      declaringFile, methodLine, methodSignature) => {
      val methodKey = MethodKey(declaringFile, methodLine,
        methodSignature)

      finder.lookupMethod(githubUrl,
        commit_id,
        methodKey) match {
        case Some(sourceCode) =>
          sender() ! MethodSrcReply(sourceCode, "")
        case none =>
          sender() ! MethodSrcReply("", "Cannot find the source code")
      }
    }
  }
}
