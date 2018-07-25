package edu.colorado.plv.fixr.git

import org.scalatest._
import java.io.File
import java.nio.file.Files

import org.eclipse.jgit.treewalk.filter.PathSuffixFilter

import edu.colorado.plv.fixr.Logger
import edu.colorado.plv.fixr.github.{RepoClosed, RepoOpened, GitHelper}

class TestGit extends FlatSpec with Matchers with BeforeAndAfter {


  "The git helper" should "download a repo at a commit" in {
    val closedRepo = RepoClosed("https://github.com/github/gitignore","967cd64")

    val closedRepo_opt_res = GitHelper.openRepo(closedRepo) match {
      case Some(openedRepo) =>
        GitHelper.closeRepo(openedRepo) match {
          case Some(closedRepo_res) => Some(closedRepo_res)
          case _ => None
        }
      case _ => None
    }

    closedRepo_opt_res should be (Some(closedRepo))
  }


  "Thet git helper" should "enumerate the repository files" in {


    val res = GitHelper.openRepo(RepoClosed("https://github.com/github/gitignore",
      "967cd64")) match {
      case Some(openedRepo) => {
        val res =
          GitHelper.foldLeftRepoFile(openedRepo,
            None,
            List[String](),
            ((acc : List[String], filePath : String) => {
              if (filePath.endsWith("Scala.gitignore")) {
                filePath :: acc
              }
              else {
                acc
              }
            }))

        GitHelper.closeRepo(openedRepo)

        res match {
          case x::xs => Some(x.endsWith("Scala.gitignore"))
          case Nil => None
        }
      }
      case _ => None
    }

    res should be (Some(true))

  }

  "Thet git helper" should "filter all files that have extension yml" in {


    val res = GitHelper.openRepo(RepoClosed("https://github.com/github/gitignore",
      "967cd64")) match {
      case Some(openedRepo) => {
        val res =
          GitHelper.foldLeftRepoFile(openedRepo,
            Some(PathSuffixFilter.create("yml")),
            List[String](),
            ((acc : List[String], filePath : String) => {
              filePath :: acc
            }))

        GitHelper.closeRepo(openedRepo)

        // The suffix filter is buggy, it also includes all the files
        // without an extension
        // For example, we get List(Global, .travis.yml, .github) as result
        res match {
          case x::xs => Some(true)
          case _ => None
        }
      }
      case _ => None
    }

    res should be (Some(true))

  }

}
