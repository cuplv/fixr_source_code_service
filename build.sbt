import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "edu.colorado.plv.fixr",
      scalaVersion := "2.12.6",
      version      := "0.0.1"
    )),
    name := "fixr_source_code_service",
    resolvers += "Maven ops4j" at "http://repository.ops4j.org/maven2",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += spooncore % Compile,
    libraryDependencies += logging_backend % Compile,
    libraryDependencies += logging % Compile
  )
