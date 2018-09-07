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
    resolvers += "Jboss ops4j" at "https://repository.jboss.org/nexus/content/repositories/deprecated",


    libraryDependencies += spooncore % Compile,
    libraryDependencies += logging_backend % Compile,
    libraryDependencies += logging % Compile,
    libraryDependencies += eclipseJgit % Compile,
//    libraryDependencies += jdt % Compile,

//    libraryDependencies += "org.eclipse.tycho" % "org.eclipse.jdt.core" % "3.14.0.v20171206-0802",
//    libraryDependencies += "org.eclipse.tycho" % "tycho-core" % "1.2.0",

    libraryDependencies += google_commons % Compile,

    libraryDependencies += akka_http % Compile,
    libraryDependencies += akka_http_json % Compile,
    libraryDependencies += akka_http_xml % Compile,
    libraryDependencies += akka_stream % Compile,

    libraryDependencies += google_java_format % Compile,


    libraryDependencies += scalaTest % Test,
    libraryDependencies += akka_htto_testkit % Test,
    libraryDependencies += akka_testkit % Test,
    libraryDependencies += akka_stream_testkit % Test,





  )
