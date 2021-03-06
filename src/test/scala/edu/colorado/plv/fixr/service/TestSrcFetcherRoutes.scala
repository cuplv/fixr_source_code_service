package edu.colorado.plv.fixr.service

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import akka.parboiled2.util.Base64

import scala.concurrent.duration._
import edu.colorado.plv.fixr.service.SrcFetcherActor._

import scala.io.Source

class TestSrcFetcherRoutes
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with SrcFetcherRoutes {

  implicit val routeTestTimeout = RouteTestTimeout(60.seconds)

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

    "patch correctly" in {
      val findMethodSrc = FindMethodSrc("https://github.com/square/retrofit",
        "684f975",
        "ParameterHandler.java",
        58,
        "apply")

      val diffsToApply =
        List(SourceDiff("+",
          DiffEntry(60, "read", "banana"),
          List(DiffEntry(0, "exit", ""))
        ))

      val expectedPatch = """@java.lang.Override
void apply(retrofit2.RequestBuilder builder, @javax.annotation.Nullable
java.lang.Object value) {
    retrofit2.Utils.checkNotNull(value, "@Url parameter is null.");
    /* [Patch start - id 0] After this method method call:
     read

    You should invoke the following methods:
    banana
     */
    builder.setRelativeUrl(value);
    /* [Patch end - id 0] before calling the method:
     exit
     */
}"""
      val pathInGit = "retrofit/src/main/java/retrofit2/ParameterHandler.java"
      val expectedRes = MethodSrcReply(
        (0,Set(expectedPatch,pathInGit)),
        "")

      val patchMethodSrc = PatchMethodSrc(findMethodSrc, diffsToApply)
      val entity = Marshal(patchMethodSrc).to[MessageEntity].futureValue
      val request = Post(uri = "/patch").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[MethodSrcReply].res._1 should ===(expectedRes.res._1)
        (responseAs[MethodSrcReply].res._2 ==(expectedRes.res._2)) should be (true)
        responseAs[MethodSrcReply] should be (expectedRes)
      }
    }
    "patch correctly with provided file" in {
      // Get test file to patch
      val sourceFile = Source.fromURL(getClass.getResource("/ParameterHandler.java")).getLines.mkString("\n")
      val sourceFileEncoded = Base64.rfc2045().encodeToString(sourceFile.toArray.map(_.toByte),true)

      val methodSrc = MethodSrc("ParameterHandler.java",58,"apply", sourceFileEncoded)

      val diffsToApply =
        List(SourceDiff("+",
          DiffEntry(60, "read", "banana"),
          List(DiffEntry(0, "exit", ""))
        ))
      val expectedPatch = """@java.lang.Override
void apply(retrofit2.RequestBuilder builder, @javax.annotation.Nullable
java.lang.Object value) {
    retrofit2.Utils.checkNotNull(value, "@Url parameter is null.");
    /* [Patch start - id 0] After this method method call:
     read

    You should invoke the following methods:
    banana
     */
    builder.setRelativeUrl(value);
    /* [Patch end - id 0] before calling the method:
     exit
     */
}"""
      val pathInGit = "ParameterHandler.java"
      val expectedRes = MethodSrcReply(
        (0,Set(expectedPatch,pathInGit)),
        "")

      val patchMethodSrc = PatchMethodFile(methodSrc, diffsToApply)
      val entity = Marshal(patchMethodSrc).to[MessageEntity].futureValue

      val request = Post(uri = "/patch_with_file").withEntity(entity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[MethodSrcReply].res._1 should ===(expectedRes.res._1)
        (responseAs[MethodSrcReply].res._2 ==(expectedRes.res._2)) should be (true)
        responseAs[MethodSrcReply] should be (expectedRes)
      }
    }
  }

}
