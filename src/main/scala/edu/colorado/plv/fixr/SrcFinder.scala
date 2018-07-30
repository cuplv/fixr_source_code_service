package edu.colorado.plv.fixr

import java.io.{File, InputStream, FileOutputStream}
import java.nio.file.{Files, Paths, Path, StandardCopyOption}
import com.google.common.io.{Files => GuavaFiles}
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter

import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap}
import edu.colorado.plv.fixr.github.{RepoClosed, RepoOpened, GitHelper}
import edu.colorado.plv.fixr.parser.ClassParser


class SrcFinder(sourceCodeMap : SourceCodeMap)  {

  def lookupMethod(github_url : String,
    commit_id : String,
    methodKey : MethodKey) : Option[Set[String]] = {

    sourceCodeMap.lookupMethod(methodKey) match {
      case Some(sourceCodeSet) => Some(sourceCodeSet)
      case None => {
        val closedRepo = RepoClosed(github_url, commit_id)
        GitHelper.openRepo(closedRepo) match {
          case Some(openRepo) => {

            Logger.debug(s"Method key ${methodKey.declaringFile}")

            // Process all files in the repo with the same name
            // We can process all the files, too
            val filter = Some(PathSuffixFilter.create(methodKey.declaringFile))
            // Alternative: process already all .java files in the repository
            // val filter = Some(PathSuffixFilter.create(".java"))
            GitHelper.foldLeftRepoFile(openRepo,
              filter,
              (),
              ((acc : Unit, res : (InputStream, String)) => {
                res match {
                  case (inputStream, filePath) =>
                    Logger.debug(s"Processing $filePath...")

                    val tmpDir : File = GuavaFiles.createTempDir()

                    val tmpFilePath = Paths.get(tmpDir.getPath(),
                      methodKey.declaringFile)

                    val fileToWrite = new File(tmpFilePath.toString)

                    try {
                      Files.copy(inputStream, fileToWrite.toPath(),
                        StandardCopyOption.REPLACE_EXISTING)

                      ClassParser.parseClassFile(
                        github_url,
                        sourceCodeMap,
                        fileToWrite.getPath)
                    } finally {
                      fileToWrite.delete
                      tmpDir.delete
                    }
                }
              }))

            sourceCodeMap.lookupMethod(methodKey) match {
              case Some(sourceCode) => Some(sourceCode)
              case None => None
            }
          }
          case None => None
        }
      }
    }
  }
}


