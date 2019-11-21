package edu.colorado.plv.fixr

import java.io.InputStream
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import edu.colorado.plv.fixr.storage._
import edu.colorado.plv.fixr.github.{GitHelper, RepoClosed}
import edu.colorado.plv.fixr.parser.{ClassParser, CommentDiff}


class SrcFinder(sourceCodeMap : SourceCodeMap)  {

  def lookupMethod(methodKey : MethodKey) : Option[(Int,Set[String])] = {
    sourceCodeMap.lookupClosestMethod(methodKey) match {
      case Some(sourceCodeSet) => Some(sourceCodeSet)
      case None => {
        val repoProcessed = processRepo(methodKey, true)

        repoProcessed match {
          case Some(x) => sourceCodeMap.lookupClosestMethod(methodKey) match {
            case Some(sourceCode) => Some(sourceCode)
            case None => None
          }
          case None => None
        }
      }
    }
  }

  def retrieveFile(methodKey: MethodKey): Option[FileInfo] = {
    Logger.debug(s"About to try patching: ${methodKey}")
    val fileInfoTmp = sourceCodeMap.lookupFileInfo(methodKey)

    fileInfoTmp match {
      case Some(x) => fileInfoTmp
      case None => {
        // try to process the repo and get the file again
        Logger.debug(s"Did not source code in cache for: ${methodKey}")
        val repoProcessed = processRepo(methodKey, true)

        Logger.debug(s"Lookup ${methodKey} again after insert...")
        sourceCodeMap.lookupFileInfo(methodKey)
      }
    }
  }
  def patchMethod(fileInfo: FileInfo, methodKey: LocalMethodKey,
    commentsDiff : Map[Int, List[CommentDiff]]) : Option[(String,String)] = { //TODO:smeier refactor this method to already have file

    // Patch the file
    Logger.debug(s"Method is in the file ${fileInfo.filePathInRepo}")

    val patch = ClassParser.parseAndPatchClassFile(methodKey,
      fileInfo, commentsDiff)

    patch match {
      case Some(patchText) => {
        require(! (fileInfo == null))
        require(! (fileInfo.filePathInRepo == null))
        require(! (patchText == null))

        Some((patchText, fileInfo.filePathInRepo))
      }
      case None => None
    }
  }

  def processRepo(methodKey : MethodKey, onlyMethodFile : Boolean) :
      Option[Boolean] = {
    val closedRepo = RepoClosed(methodKey.repoUrl, methodKey.commitId)
    GitHelper.openRepo(closedRepo) match {
      case Some(openRepo) => {
        Logger.debug(s"Trying to process the repo " +
          s" ${methodKey.repoUrl}")

        // Process all files in the repo with the same name
        // We can process all the files, too

        val filter = if (onlyMethodFile)
          Some(PathSuffixFilter.create(methodKey.declaringFile))
        else 
          // process already all .java files in the repository
          Some(PathSuffixFilter.create(".java"))

        GitHelper.foldLeftRepoFile(openRepo,
          filter,
          (),
          ((acc : Unit, res : (InputStream, String)) => {
            res match {
              case (inputStream, filePath) =>
                Logger.debug(s"Processing ${filePath}...")

                try {
                  val fileContent =
                    ClassParser.convertStreamToString(inputStream)

                  val fileInfo = RepoFileInfo(methodKey.repoUrl,
                    methodKey.commitId,
                    methodKey.declaringFile,
                    filePath,
                    fileContent)

                  // Insert the association of method and source code
                  sourceCodeMap.insertFileInfo(methodKey, fileInfo)

                  ClassParser.parseClassFile(sourceCodeMap,fileInfo)

                } finally {
                }
            }
          })) // end of fold_left on repo
        Some(true)
      }
      case None => None
    }
  } // end of process repo
}


