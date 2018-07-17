import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  val spooncore = "fr.inria.gforge.spoon" % "spoon-core" % "6.2.0"
  val logging_backend = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
}
