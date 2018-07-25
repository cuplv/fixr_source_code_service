package edu.colorado.plv.fixr.github

import java.io.File
import java.nio.file.{Path, Paths}
import com.google.common.io.Files

import org.eclipse.jgit.api.{Git, CloneCommand}
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.{TreeWalk, FileTreeIterator}

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

    try {
      val tmpDir = Files.createTempDir()

      val cloneCmd : CloneCommand =  Git.cloneRepository()

      cloneCmd.setURI(repoClosed.repoUri)
      cloneCmd.setDirectory(tmpDir)
      cloneCmd.setCloneSubmodules(false)
      val git : Git = cloneCmd.call()

      // Don't care if the repo is in a detached head
      git.checkout.setName(repoClosed.commitHash).call()

      Some(RepoOpened(git, repoClosed.repoUri, repoClosed.commitHash))

    } catch {
      case e : IllegalStateException => 
        Logger.debug("Error creating the temporary directory")
        None
      case e : Exception =>
        Logger.debug(e.getMessage)
        None
    }
  }

  /**
    * FoldLeft on the list of files in the repository.
    *
    * @warn Not thread safe for treeWalk
    */
  def foldLeftRepoFile[B](repoOpened : RepoOpened,
    accumulator: B,
    op: ((B, String) => B)) : B = {

    val repo = repoOpened.git.checkout.getRepository()
    val treeWalk = new TreeWalk(repo)

    treeWalk.addTree(new FileTreeIterator(repo))

    GitHelper.foldLeftRepoFile(treeWalk,
      accumulator,
      op)
  }

  /**
    * FoldLeft on the list of files in treeWalk
    *
    * @warn Not thread safe for treeWalk
    */
  private def foldLeftRepoFile[B](treeWalk : TreeWalk,
    accumulator: B,
    op: ((B, String) => B)) : B = {
    if (treeWalk.next()) {
      foldLeftRepoFile(treeWalk, op(accumulator, treeWalk.getPathString), op)
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
    repoOpened.git.close()
    return Some(closed)
  }

}
