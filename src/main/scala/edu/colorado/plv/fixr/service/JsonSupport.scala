package edu.colorado.plv.fixr.service

import edu.colorado.plv.fixr.service.SrcFetcherActor.{FindMethodSrc,
  SourceDiff,
  DiffEntry,
  PatchMethodSrc,
  MethodSrcReply}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val findMethodsSrcJsonFormat = jsonFormat5(FindMethodSrc)

  implicit val diffEntryJsoneFormat = jsonFormat3(DiffEntry)

  implicit val sourceDiffJsonFormat = jsonFormat3(SourceDiff)

  implicit val patchMethodSrcJsonFormat = jsonFormat2(PatchMethodSrc)


  implicit val methodSrcJsonFormat = jsonFormat2(MethodSrcReply)
}

