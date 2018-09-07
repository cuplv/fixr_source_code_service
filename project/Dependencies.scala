import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  val spooncore = "fr.inria.gforge.spoon" % "spoon-core" % "6.2.0"
  val logging_backend = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  val eclipseJgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.0.1.201506240215-r"

  val google_commons = "com.google.guava" % "guava" % "25.1-jre"

  lazy val akkaHttpVersion = "10.1.3"
  lazy val akkaVersion    = "2.5.14"

  lazy val akka_http = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akka_http_json = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  lazy val akka_http_xml = "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion
  lazy val akka_stream = "com.typesafe.akka" %% "akka-stream"% akkaVersion

  lazy val google_java_format = "com.google.googlejavaformat" % "google-java-format" % "1.6"

//test
  lazy val akka_htto_testkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
  lazy val akka_testkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  lazy val akka_stream_testkit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion

}
