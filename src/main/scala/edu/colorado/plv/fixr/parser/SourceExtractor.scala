package edu.colorado.plv.fixr.parser

import java.io.{IOException, File, BufferedReader, FileReader}
import java.util.Scanner;
import java.lang.StringBuilder;

import edu.colorado.plv.fixr.Logger

object SourceExtractor {

  /**
    * Extract the text from methodFile from position
    * startPosition to position endPosition
    */
  def extractText(methodFile : File,
    startPosition : Int,
    endPosition : Int) : Option[String] = {

    try {
      val reader = new BufferedReader(new FileReader(methodFile))

      val length = endPosition - startPosition + 1
      var readChar : Array[Char] = new Array[Char](length)

      reader.skip(startPosition)
      val rval = reader.read(readChar, 0, length)

      if (rval > 0) {
        Some(String.valueOf(readChar))
      } else {
        Logger.error("Cannot read enough characters from file")
        None
      }
    } catch {
      case ioe : IOException => {
        Logger.debug("Error reading file %s", methodFile.getName())
        None
      }
      case e : Exception => {
        Logger.debug("Exception reading file %s", methodFile.getName())
        None
      }
    }
  }

  def extractText(methodFile : File,
    startLine : Int,
    startColumn : Int,
    endLine : Int,
    endColumn : Int) : Option[String] = {

    def readLines(s : Scanner, count : Int, lines : List[String]) : List[String] = {
      s.hasNextLine() match {
        case false => lines.reverse
        case _ => {
          if (count > endLine) {
            lines.reverse
          } else if (count < startLine) {
            s.nextLine()
            readLines(s, count + 1, lines)
          } else {
            val line : String = s.nextLine()
            val startColumnIndex = if (count == startLine) startColumn else 0
            val endColumnIndex = if (count == endLine) endColumn else line.length
            val lineToCopy = line.substring(startColumnIndex, endColumnIndex)
            readLines(s, count + 1, lineToCopy :: lines)
          }
        }
      }
    }

    if (startLine > endLine) {
      Logger.error("Source code error: start line %d < " +
        "end line %d", startLine, endLine)
      None
    }
    else {
      try {
        val s = new Scanner(methodFile)

        val textLines = readLines(s, 1, List[String]())
        val stringBuilder = textLines.foldLeft (new StringBuilder(textLines.length)) ( (builder, string) => {
          builder.append(string)
          builder
        })

        val text = stringBuilder.toString()
        Some(text)

      } catch {
        case ioe : IOException => {
          Logger.debug("Error reading file %s", methodFile.getName())
          None
        }
        case e : Exception => {
          println("Exception")
          None
        }
      }
    }
  }
}
