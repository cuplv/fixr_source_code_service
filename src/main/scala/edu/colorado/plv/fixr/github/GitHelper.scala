package edu.colorado.plv.fixr.github

import java.io.{File, InputStream}
import java.nio.file.{Path, Paths}
import com.google.common.io.Files

import scala.annotation.tailrec

import org.eclipse.jgit.api.{Git, CloneCommand}
import org.eclipse.jgit.lib.{Repository, ObjectReader, ObjectId}
import org.eclipse.jgit.treewalk.{TreeWalk, FileTreeIterator}
import org.eclipse.jgit.treewalk.filter.TreeFilter

import edu.colorado.plv.fixr.Logger


abstract class RepoStatus

case class RepoClosed(repoUri : String, commitHash : String)
    extends RepoStatus

case class RepoOpened(git : Git, repoUri : String, commitHash : String)
    extends RepoStatus {
}


object GitHelper {

  /**
    * "Open" the github repository
    */
  def openRepo(repoClosed : RepoClosed) : Option[RepoOpened] = {
    Logger.debug(s"Opening repo ${repoClosed.repoUri}...")

    try {

      val repoPath  = Paths.get("/tmp",
        repoClosed.repoUri,
        repoClosed.commitHash,
        ".git")

      Logger.debug(s"PATH ${repoPath}")

      val repoGitFile = repoPath.toFile()
      val tmpDir = repoPath.getParent().toFile()
      Files.createParentDirs(repoGitFile)

      val git : Git =
        if (! repoGitFile.exists()) {
          Logger.debug(s"Repo does not exists...")
          val cloneCmd : CloneCommand =  Git.cloneRepository()

          Logger.debug(s"tmp dir ${tmpDir}")

          cloneCmd.setURI(repoClosed.repoUri)
          cloneCmd.setDirectory(tmpDir)
          cloneCmd.setCloneSubmodules(false)
          val git : Git = cloneCmd.call()
          git
        } else {
          Git.open(repoGitFile)
        }
      // Don't care if the repo is in a detached head
      git.checkout.setName(repoClosed.commitHash).call()

      Logger.debug(s"Opened repo ${repoClosed.repoUri}.")
      Some(RepoOpened(git, repoClosed.repoUri, repoClosed.commitHash))

    } catch {
      case e : IllegalStateException =>
        Logger.debug("Error creating the temporary directory")
        None
      case e : Exception =>
        Logger.debug(s"Error opening repo ${repoClosed.repoUri}...")
        Logger.debug(e.getMessage)
        None
    }
  }

  /**
    * FoldLeft on the list of files in the repository.
    *
    * Not thread safe for treeWalk
    */
  def foldLeftRepoFile[B](repoOpened : RepoOpened,
    treeFilter : Option[TreeFilter],
    accumulator: B,
    op: ((B, (InputStream, String)) => B)) : B = {

    val repo = repoOpened.git.checkout.getRepository()
    //val repo = repoOpened.git.getRepository()
    val treeWalk = new TreeWalk(repo)

    treeWalk.addTree(new FileTreeIterator(repo))

    treeFilter match {
      case  Some(filter) => treeWalk.setFilter(filter)
      case None => ()
    }

    val reader = repo.newObjectReader()
    try {
      GitHelper.foldLeftTreeWalk(treeWalk,
        reader,
        accumulator,
        op)
    }
    finally {
      reader.close
      treeWalk.close
    }
  }

  /**
    * FoldLeft on the list of files in treeWalk
    *
    * @warn Not thread safe for treeWalk
    */
  @tailrec
  private def foldLeftTreeWalk[B](treeWalk : TreeWalk,
    reader : ObjectReader,
    accumulator: B,
    op: ((B, (InputStream, String)) => B)) : B = {
    if (treeWalk.next()) {
      if (treeWalk.isSubtree()) {
        treeWalk.enterSubtree();
        foldLeftTreeWalk(treeWalk,
          reader,
          accumulator,
          op)
      }
      else {
        val objId = treeWalk.getObjectId(treeWalk.getTreeCount - 1)

        if (objId == ObjectId.zeroId()) {
          Logger.warn(s"Cannot find object for ${treeWalk.getPathString()}")

          foldLeftTreeWalk(treeWalk,
            reader,
            accumulator,
            op)
        }
        else {
          Logger.debug(s"Processing git object ${treeWalk.getPathString()}")

          val objectLoader = reader.open(objId)
          val inputStream = objectLoader.openStream()

          foldLeftTreeWalk(treeWalk,
            reader,
            op(accumulator, (inputStream, treeWalk.getPathString())),
            op)
        }
      }
    }
    else {
      treeWalk.reset()
      accumulator
    }
  }

  /**
    *  Close the repo
    */
  def closeRepo(repoOpened : RepoOpened) : Option[RepoClosed] = {
    val closed = RepoClosed(repoOpened.repoUri, repoOpened.commitHash);
    val repoDir = repoOpened.git.getRepository.getDirectory

    repoOpened.git.close()

    repoDir.delete

    return Some(closed)
  }

}
