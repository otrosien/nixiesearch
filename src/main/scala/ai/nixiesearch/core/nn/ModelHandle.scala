package ai.nixiesearch.core.nn

import io.circe.{Decoder, Encoder, Json}

import scala.util.{Failure, Success}

sealed trait ModelHandle {
  def name: String
  def asList: List[String]
}

object ModelHandle {
  def apply(ns: String, name: String) = HuggingFaceHandle(ns, name)

  case class HuggingFaceHandle(ns: String, name: String) extends ModelHandle {
    override def asList: List[String] = List(ns, name)

    override def toString: String = s"hf://$ns/$name"
  }
  case class LocalModelHandle(dir: String) extends ModelHandle {
    override def toString: String     = s"file://$dir"
    override def name: String         = dir
    override def asList: List[String] = List(dir)
  }

  val huggingFacePattern = "([a-zA-Z0-9\\-]+)/([0-9A-Za-z\\-_]+)".r
  val localPattern1      = "file:/(/.+)".r
  val localPattern2      = "file://(/.+)".r

  given modelHandleDecoder: Decoder[ModelHandle] = Decoder.decodeString.emapTry {
    case huggingFacePattern(ns, name) => Success(HuggingFaceHandle(ns, name))
    case localPattern2(path)          => Success(LocalModelHandle(path))
    case localPattern1(path)          => Success(LocalModelHandle(path))
    case other                        => Failure(InternalError(s"cannot parse model handle '$other'"))
  }

  given modelHandleEncoder: Encoder[ModelHandle] = Encoder.instance {
    case HuggingFaceHandle(ns, name) => Json.fromString(s"$ns/$name")
    case LocalModelHandle(path)      => Json.fromString(s"file://$path")
  }
}
