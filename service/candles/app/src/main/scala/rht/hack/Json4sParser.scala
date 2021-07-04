package rht.hack

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import rht.common.domain.candles.Candle

object Json4sParser {
  implicit val formats = {
    Serialization.formats(FullTypeHints(List(classOf[Candle])))
  }

  def serializer(candle: Candle) = {
    pretty(render(Extraction.decompose(candle)))
  }

  def deserializer(json: String) = {
    parse(json).extract[Candle]
  }
}
