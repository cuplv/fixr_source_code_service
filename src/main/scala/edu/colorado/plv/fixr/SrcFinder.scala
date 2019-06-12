package edu.colorado.plv.fixr

import java.io.{File, InputStream, FileOutputStream}
import java.nio.file.{Files, Paths, Path, StandardCopyOption}
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter

import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap, FileInfo}
import edu.colorado.plv.fixr.github.{RepoClosed, RepoOpened, GitHelper}
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

  def patchMethod(methodKey : MethodKey,
    commentsDiff : Map[Int, List[CommentDiff]]) : Option[(Int,String)] = {
    sourceCodeMap.lookupClosestMethod(methodKey) match {
      case Some(sourceCodeSet) => Some((1,""))
      case None => {
        val repoProcessed = processRepo(methodKey, true)

        repoProcessed match {
          case Some(x) => sourceCodeMap.lookupClosestMethod(methodKey) match {
            case Some(sourceCode) => {
              // We do have a file and a method we can process that.
              // We do not really care about the source code here, because we
              // have to run spoon on the file again.

             // ClassParser.parseAndPatchClassFile(
              None
            }
            case None => None
          }
          case None => None
        }
      }
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

                  val fileInfo = FileInfo(methodKey.repoUrl,
                    methodKey.commitId,
                    methodKey.declaringFile,
                    filePath,
                    fileContent)

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


