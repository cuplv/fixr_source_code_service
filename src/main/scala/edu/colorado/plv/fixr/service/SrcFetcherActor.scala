package edu.colorado.plv.fixr.service


import edu.colorado.plv.fixr.{SrcFinder, Logger}
import edu.colorado.plv.fixr.storage.{MethodKey,MemoryMap}
import edu.colorado.plv.fixr.parser.{CommentDiff}

import akka.actor.{Actor, ActorLogging, Props}

import java.lang.StringBuilder

import com.google.googlejavaformat.java.{Formatter, JavaFormatterOptions}
import com.google.googlejavaformat.java.{SnippetFormatter, Replacement}
import com.google.googlejavaformat.java.SnippetFormatter.SnippetKind
import com.google.common.collect.ImmutableList
import com.google.common.collect.Range

import scala.annotation.tailrec

object SrcFetcherActor {
  final case class FindMethodSrc(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodName : String)

  final case class DiffEntry(
    lineNum : Int,
    entryName : String,
    what : String)

  final case class SourceDiff(
    diffType : String,
    entry : DiffEntry,
    exits : List[DiffEntry]
  )

  final case class PatchMethodSrc(methodRef : FindMethodSrc,
    diffsToApply : List[SourceDiff])

  final case class MethodSrcReply(res : (Int,Set[String]),
    errorDesc : String)

  def props : Props = Props[SrcFetcherActor]
}

class SrcFetcherActor extends Actor with ActorLogging {
  import SrcFetcherActor._

  val finder = new SrcFinder(new MemoryMap())

  def receive : Receive = {
    case FindMethodSrc(githubUrl, commitId,
      declaringFile, methodLine, methodName) => {

      Logger.debug("Receive request:" +
        s" - githubUrl = $githubUrl" +
        s" - commitId = $commitId" +
        s" - declaringFile = $declaringFile" +
        s" - methodLine = $methodLine" +
        s" - methodName = $methodName")

      validateData(githubUrl, commitId, declaringFile,
        methodLine, methodName) match {
        case Some(element) => sender() ! element
        case None => {
          val methodKey = MethodKey(githubUrl,
            commitId,
            declaringFile,
            methodLine,
            methodName)

          finder.lookupMethod(methodKey) match {
            case Some(lookupResult) =>
              // Robust handling for indentation
              var processedResult =
                try {
                  lookupResult
                } catch {
                  case e : Exception => lookupResult
                }

              sender() ! MethodSrcReply(processedResult, "")
            case None =>
              sender() ! MethodSrcReply((-1,Set()),
                "Cannot find the source code")
          }
        }
      }
    }

    case PatchMethodSrc(findMethodSrc, diffsToApply) => {
      validateData(findMethodSrc) match {
        case Some(element) => sender() ! element
        case None => {
          validateData(diffsToApply) match {
            case Some(element) => sender() ! element
            case None => {

              val commentDiffs = CreatePatchText.processDiffs(diffsToApply)

              Logger.debug(s"Transformed commentDiffs: ${commentDiffs}")

              val methodKey = MethodKey(findMethodSrc.githubUrl,
                findMethodSrc.commitId,
                findMethodSrc.declaringFile,
                findMethodSrc.methodLine,
                findMethodSrc.methodName)

              val patchRes = finder.patchMethod(methodKey, commentDiffs)

              patchRes match {
                case Some((patchText, filePath)) =>
                  // Hack --- should change the message
                  sender() ! MethodSrcReply(
                    (0, Set(patchText, filePath)),
                    "")
                case None =>
                  sender() ! MethodSrcReply((-1, Set()),
                    "Cannot find the source code")
              }
            }
          }
        }
      }
    }
  }


  /** Post-process the found source code (e.g. indenting it)
    *
    */
  // private def prettify(lookupResult : (Int,Set[String])) : (Int,Set[String]) = {
  //   lookupResult match {
  //     case (opt_value, sourceCodeSet) =>
  //       (opt_value, sourceCodeSet.map( elem => {
  //         val formatterOptions = JavaFormatterOptions.defaultOptions()
  //         val formatter = new Formatter(formatterOptions)

  //         // Builds the dummy class
  //         val buf : StringBuffer = new StringBuffer("class Dummy {\n")
  //         buf.append(elem)
  //         buf.append("\n}")

  //         val formattedWithDummy = formatter.formatSource(buf.toString)

  //         // Remove the dummy string and fix the indentation
  //         def removeDummy(buf : StringBuffer,
  //           list : List[String],
  //           indentToRemove : Int) : StringBuffer = {
  //           list match {
  //             case line :: xs => {
  //               val newLine =
  //               if (line.length > indentToRemove) {
  //                 line.substring(indentToRemove, line.length())
  //               } else {
  //                 line // may be empty
  //               }
  //               buf.append(newLine)
  //               buf.append("\n")

  //             removeDummy(buf, xs, indentToRemove)
  //             }
  //             case Nil => buf
  //           }
  //         }

  //         val strList : List[String] =
  //           formattedWithDummy.lines.foldLeft(List[String]()) ((r,c) => c :: r).reverse
  //         val sliced = strList.slice(1,strList.length-1)

  //         removeDummy(new StringBuffer(),
  //           sliced, 2).toString()
  //       }))
  //   }
  // }

  private def validateData(findMethodSrc : FindMethodSrc) :
      Option[MethodSrcReply] = {
    findMethodSrc match {
      case FindMethodSrc(githubUrl, commitId,
        declaringFile, methodLine, methodName) => validateData(githubUrl,
          commitId, declaringFile, methodLine, methodName)
      case _ => Some(MethodSrcReply((-1,Set()), "Wrong format for findMethodSrc"))
    }
  }

  private def validateData(githubUrl : String,
    commitId : String,
    declaringFile : String,
    methodLine : Int,
    methodName : String) : Option[MethodSrcReply] = {
      githubUrl match {
        case "" => Some(MethodSrcReply((-1,Set()), "Empty github url"))
        case _ =>
          commitId match {
            case "" => Some(MethodSrcReply((-1,Set()), "Empty commit id"))
            case _ =>
              declaringFile match {
                case "" => Some(MethodSrcReply((-1,Set()), "Empty declaring file"))
                case _ =>
                  methodName match {
                    case "" => Some(MethodSrcReply((-1,Set()), "Empty method name"))
                    case _ => if (methodLine <= 0)
                      Some(MethodSrcReply((-1,Set()), "Negative method line"))
                    else
                      None
                  }
              }
          }
      }
  }

  private def validateData(diffsToApply : List[SourceDiff]) :
      Option[MethodSrcReply] = {
    // TODO
    None
  }
}

object CreatePatchText {
  def processDiffs(sourceDiffs : List[SrcFetcherActor.SourceDiff]) :
      Map[Int, List[CommentDiff]] = {

    def processDiffEntry(diffEntry : SrcFetcherActor.DiffEntry,
      sourceDiffNum : Int,
      isEntry : Boolean,
      isAdd : Boolean,
      lineToDiffs : Map[Int, List[CommentDiff]]) :
        Map[Int, List[CommentDiff]] = {

      val action = if (isAdd) "invoke" else "remove"
      val diffText =
        if (isEntry) {
          val entryName = s"[${sourceDiffNum}] After this method method call (${diffEntry.entryName})"
          val changeExplanation = s"You should ${action} the following methods:\n${diffEntry.what}"
          s"${entryName}\n${changeExplanation}"
        } else {
          s"[${sourceDiffNum}] The change should ends here (before calling the method ${diffEntry.entryName})"
        }


      val commentDiff = CommentDiff(sourceDiffNum,
        diffEntry.lineNum,
        diffText,
        isAdd, isEntry)

      val res = lineToDiffs.get(diffEntry.lineNum)
      res match {
        case Some(list) =>
          lineToDiffs + (diffEntry.lineNum -> (commentDiff :: list))
        case None =>
          lineToDiffs + (diffEntry.lineNum -> List(commentDiff))
      }
    }

    def processSourceDiff(sourceDiff : SrcFetcherActor.SourceDiff,
      sourceDiffNum : Int,
      lineToDiffs : Map[Int, List[CommentDiff]]) :
        Map[Int, List[CommentDiff]] = {

      val isAdd = sourceDiff.diffType == "+"

      val newDiffs = processDiffEntry(sourceDiff.entry,
        sourceDiffNum, true, isAdd, lineToDiffs)

      sourceDiff.exits.foldLeft(newDiffs)( (acc, diffEntry) => {
        processDiffEntry(diffEntry, sourceDiffNum, false, isAdd, newDiffs)
      })
    }

    @tailrec
    def processDiffsRec(sourceDiffs : List[SrcFetcherActor.SourceDiff],
      sourceDiffNum : Int,
      lineToDiffs : Map[Int, List[CommentDiff]]) :
        Map[Int, List[CommentDiff]] = {

      sourceDiffs match {
        case x::xs => {
          processDiffsRec(xs, sourceDiffNum + 1,
            processSourceDiff(x, sourceDiffNum, lineToDiffs))
        }
        case Nil => {
          lineToDiffs
        }
      }
    }


    processDiffsRec(sourceDiffs, 0, Map())
  }
}
