package edu.colorado.plv.fixr

import java.io.{File, InputStream, FileOutputStream}
import java.nio.file.{Files, Paths, Path, StandardCopyOption}
import com.google.common.io.{Files => GuavaFiles}
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


  // def patchMethod(github_url : String,
  //   commit_id : String,
  //   methodKey : MethodKey,
  //   commentsDiff : Map[Int, List[CommentDiff]]) : Option[(Int,Set[String])] = {

  //   // Finds or insert the method in the table
  //   val foundMethods = self.lookupMethod(github_url, commit_id, methodKey)

  //   match 

  // def parseAndPatchClassFile(gitHubUrl, sourceCodeMap,
  //   : String,
  //   sourceCodeMap : SourceCodeMap,
  //   inputFileName : String,
  //   methodKey : MethodKey,

  //   sourceCodeMap.lookupClosestMethod(methodKey) match {
  //     case Some(sourceCodeSet) => Some(sourceCodeSet)
  //     case None => {
  //       val closedRepo = RepoClosed(github_url, commit_id)
  //       GitHelper.openRepo(closedRepo) match {
  //         case Some(openRepo) => {

  //           Logger.debug(s"Trying to find and insert method " +
  //             s"key ${methodKey.declaringFile}")

  //           // Process all files in the repo with the same name
  //           // We can process all the files, too
  //           val filter = Some(PathSuffixFilter.create(methodKey.declaringFile))
  //           // Alternative: process already all .java files in the repository
  //           // val filter = Some(PathSuffixFilter.create(".java"))
  //           GitHelper.foldLeftRepoFile(openRepo,
  //             filter,
  //             (),
  //             ((acc : Unit, res : (InputStream, String)) => {
  //               res match {
  //                 case (inputStream, filePath) =>
  //                   Logger.debug(s"Processing $filePath...")

  //                   val tmpDir : File = GuavaFiles.createTempDir()

  //                   val tmpFilePath = Paths.get(tmpDir.getPath(),
  //                     methodKey.declaringFile)

  //                   val fileToWrite = new File(tmpFilePath.toString)

  //                   try {
  //                     Files.copy(inputStream, fileToWrite.toPath(),
  //                       StandardCopyOption.REPLACE_EXISTING)

  //                     ClassParser.parseClassFile(
  //                       github_url,
  //                       sourceCodeMap,
  //                       fileToWrite.getPath)
  //                   } finally {
  //                     fileToWrite.delete
  //                     tmpDir.delete
  //                   }
  //               }
  //             }))

  //           // Get the previously inserted method
  //           sourceCodeMap.lookupClosestMethod(methodKey) match {
  //             case Some(sourceCode) => Some(sourceCode)
  //             case None => None
  //           }
  //         }
  //         case None => None
  //       }
  //     }
  //   }
  // }

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
                Logger.debug(s"Processing $filePath...")

                val tmpDir : File = GuavaFiles.createTempDir()

                val tmpFilePath = Paths.get(tmpDir.getPath(), filePath)
                val fileToWrite = new File(tmpFilePath.toString)
                GuavaFiles.createParentDirs(fileToWrite)

                try {
                  Files.copy(inputStream, fileToWrite.toPath(),
                    StandardCopyOption.REPLACE_EXISTING)

                  val fileContent =
                    new String(Files.readAllBytes(fileToWrite.toPath()))

                  val fileInfo = FileInfo(methodKey.repoUrl,
                    methodKey.commitId,
                    methodKey.declaringFile,
                    filePath,
                    fileContent)

                  ClassParser.parseClassFile(
                    methodKey.repoUrl,
                    sourceCodeMap,
                    fileToWrite.getPath,
                    fileInfo
                  )

                } finally {
                  fileToWrite.delete
                  tmpDir.delete
                }
            }
          })) // end of fold_left on repo
        Some(true)
      }
      case None => None
    }
  } // end of process repo
}


