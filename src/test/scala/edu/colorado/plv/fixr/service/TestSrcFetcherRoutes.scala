package edu.colorado.plv.fixr.service

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import edu.colorado.plv.fixr.service.SrcFetcherActor.{FindMethodSrc,
  MethodSrcReply}

class TestSrcFetcherRoutes
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with SrcFetcherRoutes {

  override val srcFetcherActor: ActorRef =
    system.actorOf(SrcFetcherActor.props, "srcFetcherActor")

  lazy val routes = srcFetcherRoutes

  "SrcFetcherRoutes" should {
    "return no method exists" in {
      val findMethodSrc = FindMethodSrc("", "",
        "", 1, "")
      val entity = Marshal(findMethodSrc).to[MessageEntity].futureValue

      val request = HttpRequest(uri = "/src").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        responseAs[MethodSrcReply] ===(MethodSrcReply("","Cannot find the source code"))
      }
    }
  }


}
