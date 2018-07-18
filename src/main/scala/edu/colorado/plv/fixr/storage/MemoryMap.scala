package edu.colorado.plv.fixr.storage

import scala.collection.mutable.HashMap;

class MemoryMap extends SourceCodeMap {

  val map : HashMap[MethodKey, String] = new HashMap[MethodKey, String]()

  def insertMethod(key : MethodKey, methodText : String) : Unit = {
    map.update(key, methodText)
  }

  def lookupMethod(key : MethodKey) : Option[String] = {
    map.get(key)
  }

}
