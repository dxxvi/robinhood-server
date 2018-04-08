package home

import java.time.LocalDateTime

import spray.json._

object Quote extends DefaultJsonProtocol {
    implicit object QuoteJsonFormat extends RootJsonFormat[Quote] {
        override def write(obj: Quote): JsValue = serializationError("No need to serialize Quote")

        override def read(json: JsValue): Quote = {
            val fields = json.asJsObject("check your quote json string").fields
            val year: Int = fields.get("year").collect({ case JsNumber(y) => y.toInt }).head
            val month: Int = fields.get("month").collect({ case JsNumber(y) => y.toInt }).head
            val date: Int = fields.get("date").collect({ case JsNumber(y) => y.toInt }).head
            val hour: Int = fields.get("hour").collect({ case JsNumber(y) => y.toInt }).head
            val minute: Int = fields.get("minute").collect({ case JsNumber(y) => y.toInt }).head
            val second: Int = fields.get("second").collect({ case JsNumber(y) => y.toInt }).head
            val q: Double = fields.get("quote").collect({ case JsNumber(y) => y.toDouble }).head
            Quote(LocalDateTime.of(year, month, date, hour, minute, second), q)
        }
    }
}

case class Quote(ldt: LocalDateTime, q: Double) {
}
