package edu.colorado.plv.fixr.parser

import edu.colorado.plv.fixr.service.SrcFetcherActor.{SourceDiff, DiffEntry}
import scala.annotation.tailrec

final case class CommentDiff(
  sourceDiffNum : Int,
  lineNum : Int,
  diffText : String,
  isAdd : Boolean
)

object CreatePatchText {
  def processDiffs(sourceDiffs : List[SourceDiff]) = {

    def processDiffEntry(diffEntry : DiffEntry,
      sourceDiffNum : Int,
      isAdd : Boolean,
      lineToDiffs : Map[Int, List[CommentDiff]]) : Map[Int, List[CommentDiff]] = {

      val diffText = ""

      val commentDiff = CommentDiff(sourceDiffNum,
        diffEntry.lineNum,
        diffText,
        isAdd)

      val res = lineToDiffs.get(diffEntry.lineNum)
      res match {
        case Some(list) =>
          lineToDiffs + (diffEntry.lineNum -> (commentDiff :: list))
        case None =>
          lineToDiffs + (diffEntry.lineNum -> List(commentDiff))
      }
    }

    def processSourceDiff(sourceDiff : SourceDiff,
      sourceDiffNum : Int,
      lineToDiffs : Map[Int, List[CommentDiff]]) : Map[Int, List[CommentDiff]] = {

      val isAdd = sourceDiff.diffType == "+"

      val newDiffs = processDiffEntry(sourceDiff.entry, sourceDiffNum,
        isAdd, lineToDiffs)

      sourceDiff.exits.foldLeft(newDiffs)( (acc, diffEntry) => {
        processDiffEntry(diffEntry, sourceDiffNum, isAdd, newDiffs)
      })
    }

    @tailrec
    def processDiffsRec(sourceDiffs : List[SourceDiff], sourceDiffNum : Int,
      lineToDiffs : Map[Int, List[CommentDiff]]) : Map[Int, List[CommentDiff]] = {

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
