package edu.colorado.plv.fixr.service

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

object SrcServer extends App with SrcFetcherRoutes {
  val config = ConfigFactory.load()
  val actor_system_name = config.getString(
    "edu.colorado.plv.fixr.fixr_source_code_service.actor_system_name")
  val host = config.getString(
    "edu.colorado.plv.fixr.fixr_source_code_service.host")
  val port = config.getInt("akka.http.server.default-http-port")

  implicit val system: ActorSystem = ActorSystem(actor_system_name)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Bootstrap the service
  val srcFetcherActor: ActorRef = system.actorOf(SrcFetcherActor.props, "srcFetcherActor")
  lazy val routes: Route = srcFetcherRoutes

  Http().bindAndHandle(routes, host, port)
  println(s"Server online at http://$host:$port/")

  Await.result(system.whenTerminated, Duration.Inf)
}
