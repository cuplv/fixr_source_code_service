package edu.colorado.plv.fixr.parser

import edu.colorado.plv.fixr.service.SrcFetcherActor.{SourceDiff, DiffEntry}
import scala.annotation.tailrec

final case class CommentDiff(
  sourceDiffNum : Int,
  lineNum : Int,
  diffText : String,
  isAdd : Boolean,
  isMultiLine : Boolean
)

object CreatePatchText {
  def processDiffs(sourceDiffs : List[SourceDiff]) :
      Map[Int, List[CommentDiff]] = {

    def processDiffEntry(diffEntry : DiffEntry,
      sourceDiffNum : Int,
      isEntry : Boolean,
      isAdd : Boolean,
      lineToDiffs : Map[Int, List[CommentDiff]]) :
        Map[Int, List[CommentDiff]] = {

      val action = if (isAdd) "invoke" else "remove"
      val diffText =
        if (isEntry) {
          val entryName = s"[${sourceDiffNum}] After this method method call (${diffEntry.entryName})"
          val changeExplanation = s"Yous should ${action} the following methods\n: ${diffEntry.what}"
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

    def processSourceDiff(sourceDiff : SourceDiff, sourceDiffNum : Int,
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
    def processDiffsRec(sourceDiffs : List[SourceDiff], sourceDiffNum : Int,
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
