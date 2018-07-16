import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "edu.colorado.plv.fixr",
      scalaVersion := "2.12.6",
      version      := "0.0.1"
    )),
    name := "fixr_source_code_service",
    libraryDependencies += scalaTest % Test
  )
