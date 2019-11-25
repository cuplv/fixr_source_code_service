package edu.colorado.plv.fixr.storage

object MethodKey {

  def apply(repoUrl:String, commitId: String, declaringFile : String, startLine:Int, methodName:String): MethodKey =
    MethodKey(repoUrl,commitId,LocalMethodKey(declaringFile, startLine, methodName))
}

case class MethodKey(
  repoUrl : String,
  commitId : String,
  localMethodKey: LocalMethodKey) {
    def declaringFile = localMethodKey.declaringFile
    def startLine = localMethodKey.startLine
    def methodName = localMethodKey.methodName
  }
case class LocalMethodKey(
  declaringFile : String,
  startLine : Int,
  methodName : String)

abstract class FileInfo{
  def filePathInRepo :String
  def declaringFile : String
  def fileContent : String
}

/**
  * FileInfo used when caching is desired
  * caching is used to avoid re-downloading github repositories
  */
case class RepoFileInfo(
  repoUrl : String,
  commitId : String,
  declaringFile_ : String,
  filePathInRepo_ : String,
  fileContent_ : String) extends FileInfo {
  override def filePathInRepo : String = filePathInRepo_
  override def declaringFile = declaringFile_
  override def fileContent = fileContent_
}

case class NoCacheFileInfo(
  declaringFile_ : String,
  fileContent_ : String) extends FileInfo{
  override def filePathInRepo() : String = declaringFile_
  override def declaringFile() : String = declaringFile_
  override def fileContent: String = fileContent_
}

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
  def lookupFileInfo(key : MethodKey) : Option[FileInfo]

  def clear() : Unit
}

