package edu.colorado.plv.fixr.storage

case class MethodKey(declaringFile : String,
  startLine : Int,
  signature : String)

/**
  * Defines the interface to the storage used to save the extracted
  * source code
  */
trait SourceCodeMap {
  def insertMethod(key : MethodKey, methodText : String) : Unit
  def lookupMethod(key : MethodKey) : Option[String]
  def clear() : Unit
}

