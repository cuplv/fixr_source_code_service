package edu.colorado.plv.fixr.service

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._

import edu.colorado.plv.fixr.service.SrcFetcherActor.{FindMethodSrc,
  MethodSrcReply}

class TestSrcFetcherRoutes
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with SrcFetcherRoutes {

  implicit val routeTestTimeout = RouteTestTimeout(30.seconds)

  override val srcFetcherActor: ActorRef =
    system.actorOf(SrcFetcherActor.props, "srcFetcherActor")

  lazy val routes = srcFetcherRoutes

  "SrcFetcherRoutes" should {
    "return no method exists" in {
      val findMethodSrc = FindMethodSrc("", "",
        "", 1, "")
      val entity = Marshal(findMethodSrc).to[MessageEntity].futureValue

      val request = Post(uri = "/src").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        responseAs[MethodSrcReply] should ===(MethodSrcReply((-1,Set()),"Empty github url"))
      }
    }

    "return no method exists again" in {
      val findMethodSrc = FindMethodSrc("https://github.com/github/gitignore",
        "967cd64",
        "Pippo.java", 1, "int m()")
      val entity = Marshal(findMethodSrc).to[MessageEntity].futureValue

      val request = Post(uri = "/src").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        responseAs[MethodSrcReply] should ===(MethodSrcReply((-1,Set()),
          "Cannot find the source code"))
      }
    }

    "find createRawCall method" in {
      val findMethodSrc = FindMethodSrc("https://github.com/square/retrofit",
        "684f975",
        "OkHttpCall.java",
        189,
        "createRawCall")
      val entity = Marshal(findMethodSrc).to[MessageEntity].futureValue

      val request = Post(uri = "/src").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)


        val createRawCall = """private okhttp3.Call createRawCall() throws IOException {
    okhttp3.Call call = callFactory.newCall(requestFactory.create(args));
    if (call == null) {
      throw new NullPointerException("Call.Factory returned null.");
    }
    return call;
  }"""
        responseAs[MethodSrcReply] should ===(MethodSrcReply((189,Set(createRawCall)),""))
      }
    }

    "find read method" in {
      val findMethodSrc = FindMethodSrc("https://github.com/square/retrofit",
        "684f975",
        "OkHttpCall.java",
        294,
        "read")
      val entity = Marshal(findMethodSrc).to[MessageEntity].futureValue

      val request = Post(uri = "/src").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)


        val readRawCall = """@Override public long read(Buffer sink, long byteCount) throws IOException {
          try {
            return super.read(sink, byteCount);
          } catch (IOException e) {
            thrownException = e;
            throw e;
          }
        }"""

        responseAs[MethodSrcReply] should ===(MethodSrcReply((294,Set(readRawCall)),""))
      }
    }

  }


}
