package edu.colorado.plv.fixr.service

import akka.actor.{Actor, ActorLogging, Props}


object SrcFetcherActor {
  final case class FindMethodSrc(githubUrl : String,
    methodLine : Int,
    methodSignature : String)
  final case class MethodSrcReply(src : String, errorDesc : String)

  def props : Props = Props[SrcFetcherActor]
}

class SrcFetcherActor extends Actor with ActorLogging {
  import SrcFetcherActor._

  def receive : Receive = {
    case FindMethodSrc(githubUrl, methodLine, methodSignature) =>
      sender() ! MethodSrcReply("", "Not implemented")
  }
}
