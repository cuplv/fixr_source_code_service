package edu.colorado.plv.fixr.service

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object SrcServer extends App with SrcFetcherRoutes {

  // TODO: config
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Bootstrap the service
  val srcFetcherActor: ActorRef = system.actorOf(SrcFetcherActor.props, "srcFetcherActor")
  lazy val routes: Route = srcFetcherRoutes


  // Start the http server
  // TODO: from config
  Http().bindAndHandle(routes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)


}
