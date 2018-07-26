package edu.colorado.plv.fixr.service

import edu.colorado.plv.fixr.service.SrcFetcherActor.{FindMethodSrc,
  MethodSrcReply}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val findMethodsSrcJsonFormat = jsonFormat3(FindMethodSrc)
  implicit val methodSrcJsonFormat = jsonFormat2(MethodSrcReply)
}

