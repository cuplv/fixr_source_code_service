import Dependencies._

enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "edu.colorado.plv.fixr",
      scalaVersion := "2.12.6",
      version      := "0.0.1"
    )),
    name := "fixr_source_code_service",
    resolvers += "Maven ops4j" at "http://repository.ops4j.org/maven2",

    libraryDependencies += spooncore % Compile,
    libraryDependencies += logging_backend % Compile,
    libraryDependencies += logging % Compile,
    libraryDependencies += eclipseJgit % Compile,
    libraryDependencies += google_commons % Compile,

    libraryDependencies += akka_http % Compile,
    libraryDependencies += akka_http_json % Compile,
    libraryDependencies += akka_http_xml % Compile,
    libraryDependencies += akka_stream % Compile,

    libraryDependencies += scalaTest % Test,
    libraryDependencies += akka_htto_testkit % Test,
    libraryDependencies += akka_testkit % Test,
    libraryDependencies += akka_stream_testkit % Test
  )
