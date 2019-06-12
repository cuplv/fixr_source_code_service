package edu.colorado.plv.fixr.storage

import scala.collection.mutable.{Map,HashMap};
import scala.math.abs
import edu.colorado.plv.fixr.Logger

case class KeyNoLine(
  repoUrl : String,
  declaringFile : String,
  methodName : String)


class MemoryMap extends SourceCodeMap {

  val map : Map[KeyNoLine, Map[Int, Set[String]]] =
    new HashMap[KeyNoLine, Map[Int, Set[String]]]()

  val maxLines = 4

  def lookupLineMap(key : MethodKey) : Option[Map[Int, Set[String]]] = {
    val keyNoLine =  KeyNoLine(key.repoUrl,
      key.declaringFile, key.methodName);

    if (map.contains(keyNoLine)) {
      Some(map(keyNoLine))
    } else {
      None
    }
  }

  def lookupSet(key : MethodKey,
    mapLn : Map[Int, Set[String]]) : Option[Set[String]] = {

    if (mapLn.contains(key.startLine)) {
      Some(mapLn(key.startLine))
    } else {
      None
    }
  }

  def insertMethod(key : MethodKey, methodText : String) : Unit = {
    val mapLn =
      this.lookupLineMap(key) match {
        case Some(mapLn) => mapLn
        case None => {
          val keyNoLine =  KeyNoLine(key.repoUrl, key.declaringFile,
            key.methodName);
          val mapLn = new HashMap[Int, Set[String]]()
          map.update(keyNoLine, mapLn)
          mapLn
        }
      }

    val currentSet =
      this.lookupSet(key, mapLn) match {
        case Some(currentSet) => currentSet
        case None => {
          val currentSet = Set[String]()
          mapLn.update(key.startLine, currentSet)
          currentSet
        }
      }

    mapLn.update(key.startLine, currentSet + methodText)
  }

  def lookupMethod(key : MethodKey) : Option[Set[String]] = {
    this.lookupLineMap(key) match {
      case Some(mapLn) => {
        this.lookupSet(key, mapLn) match {
          case Some(currentSet) => {
            Some(currentSet)
          }
          case None => None
        }
      }
      case None => None
    }
  }

  def lookupClosestMethod(key : MethodKey) : Option[(Int,Set[String])] = {
    val optValue : Option[(Int,Set[String])] =
      this.lookupLineMap(key) match {
        case Some(mapLn) => {
          val initVal : Option[(Int,Set[String])] = None
          mapLn.foldLeft(initVal) {
            case (optimal, (startLine, methodSet)) => {
              val current_val = math.abs(key.startLine - startLine)

              optimal match {
                case Some(opt_pair) =>
                  opt_pair match {
                    case (optimalLine, opt_set)
                        if (math.abs(key.startLine - optimalLine)) > current_val => {
                          Some((startLine, methodSet))
                        }
                    case _ => Some(opt_pair)
                  }
                case None => {
                  Some((startLine, methodSet))
                }
              }
            }
          }
        }
        case None => None
      }

    optValue match {
      case Some(opt) => opt match {
        case (optimal, s) => Some((optimal, s))
        case _ => None
      }
      case None => None
    }

  }

  def clear() : Unit = {
    map.clear()
  }

  def insertFileInfo(key : MethodKey, fileInfo : FileInfo) = {
    // TODO
  }

  def loopupFileInfo(key : MethodKey) : Option[String] = {
    None
  }

  def clearFile() : Unit = {
  }
}
