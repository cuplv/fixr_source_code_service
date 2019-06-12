package edu.colorado.plv.fixr.storage

case class MethodKey(
  repoUrl : String,
  commitId : String,
  declaringFile : String,
  startLine : Int,
  methodName : String)

case class FileInfo(
  repoUrl : String,
  commitId : String,
  declaringFile : String,
  filePathInRepo : String,
  fileContent : String)

/**
  * Defines the interface to the storage used to save the extracted
  * source code
  * 
  * The trait should be called cache instead of map --- we do not want to
  * keep in memory all the results!
  */
trait SourceCodeMap {
  def insertMethod(key : MethodKey, methodText : String) : Unit
  def lookupMethod(key : MethodKey) : Option[Set[String]]
  def lookupClosestMethod(key : MethodKey) : Option[(Int,Set[String])];

  def insertFileInfo(key : MethodKey, fileInfo : FileInfo): Unit
  def loopupFileInfo(key : MethodKey) : Option[String]

  def clear() : Unit
}

