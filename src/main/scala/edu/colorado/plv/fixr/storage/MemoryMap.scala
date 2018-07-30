package edu.colorado.plv.fixr.storage

import scala.collection.mutable.HashMap;

class MemoryMap extends SourceCodeMap {

  val map : HashMap[MethodKey, Set[String]] =
    new HashMap[MethodKey, Set[String]]()

  def insertMethod(key : MethodKey, methodText : String) : Unit = {
    if (map.contains(key)) {
      val keySet = map(key)
      map.update(key, keySet + methodText)
    } else {
      map.update(key, Set(methodText))
    }
  }

  def lookupMethod(key : MethodKey) : Option[Set[String]] = {
    map.get(key)
  }

  def clear() : Unit = {
    map.clear()
  }
}
