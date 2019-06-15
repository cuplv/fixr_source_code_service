package edu.colorado.plv.fixr.parser

final case class CommentDiff(
  sourceDiffNum : Int,
  lineNum : Int,
  diffText : String,
  isAdd : Boolean,
  isMultiLine : Boolean
)

