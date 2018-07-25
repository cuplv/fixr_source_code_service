package edu.colorado.plv.fixr

import com.typesafe.scalalogging.{Logger => ScalaLogger}

object Logger {
  val logger = ScalaLogger("source_code_service")

  def debug(message : String) : Unit = logger.debug(message)
  def debug(message : String, args: Any*) : Unit = logger.debug(message, args)

  def error(message : String) : Unit = logger.error(message)
  def error(message : String, args: Any*) : Unit = logger.error(message, args)

  def warn(message : String) : Unit = logger.warn(message)
  def warn(message : String, args: Any*) : Unit = logger.warn(message, args)
}
