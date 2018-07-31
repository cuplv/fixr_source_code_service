package edu.colorado.plv.fixr.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

trait SrcFetcherRoutes extends JsonSupport {
  import SrcFetcherActor._

  implicit def system : ActorSystem

  lazy val log = Logging(system, classOf[SrcFetcherRoutes])

  def srcFetcherActor: ActorRef

  // TODO: set the timeout via config file
  // implicit lazy val timeout = ConfigFactory.load().getDuration("akka.http.server.request-timeout") // Timeout(60.seconds)
  implicit lazy val timeout = Timeout(60.seconds)

  lazy val srcFetcherRoutes: Route = 
    path("src") {
      post {
        entity(as[FindMethodSrc]) { findMethodRequest =>
          val maybeFindMethodSrc : Future[MethodSrcReply] =
            (srcFetcherActor ? findMethodRequest).mapTo[MethodSrcReply]
          rejectEmptyResponse {
            complete(maybeFindMethodSrc)
          }
        }
      }
    }
}


