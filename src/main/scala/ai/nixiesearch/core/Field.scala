package ai.nixiesearch.core

sealed trait Field {
  def name: String
}

object Field {
  case class TextField(name: String, value: String)           extends Field
  case class TextListField(name: String, value: List[String]) extends Field
  case class IntField(name: String, value: Int)               extends Field
}
