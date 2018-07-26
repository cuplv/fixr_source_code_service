package edu.colorado.plv.fixr

import edu.colorado.plv.fixr.storage.{MethodKey, SourceCodeMap}
import edu.colorado.plv.fixr.github.{RepoClosed, RepoOpened, GitHelper}
import edu.colorado.plv.fixr.parser.ClassParser
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter

class SrcFinder(sourceCodeMap : SourceCodeMap)  {

  def lookupMethod(github_url : String,
    commit_id : String,
    methodKey : MethodKey) : Option[String] = {

    sourceCodeMap.lookupMethod(methodKey) match {
      case Some(sourceCode) => Some(sourceCode)
      case None => {
        val closedRepo = RepoClosed(github_url, commit_id)
        GitHelper.openRepo(closedRepo) match {
          case Some(openRepo) => {
            // Process all files in the repo with the same name
            // We can process all the files, too
            val filter = Some(PathSuffixFilter.create(methodKey.declaringFile))
            // Alternative: process all
            // val filter = None
            GitHelper.foldLeftRepoFile(openRepo,
              filter,
              (),
              ((acc : Unit, filePath : String) => {
                ClassParser.parseClassFile(sourceCodeMap,
                  filePath)
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


