package edu.colorado.plv.fixr.storage

case class MethodKey(
  repoUrl : String,
  declaringFile : String,
  startLine : Int,
  methodName : String)

/**
  * Defines the interface to the storage used to save the extracted
  * source code
  */
trait SourceCodeMap {
  def insertMethod(key : MethodKey, methodText : String) : Unit
  def lookupMethod(key : MethodKey) : Option[Set[String]]

  def lookupClosestMethod(repoUrl, declaringFile,
    startLine, methodkey) : Option[String];

  def clear() : Unit
}

