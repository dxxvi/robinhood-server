package home

import spray.json._

object FakePosition extends DefaultJsonProtocol {
    implicit object FakePositionJsonFormat extends RootJsonFormat[FakePosition] {
        override def write(p: FakePosition): JsValue = JsObject(
            "previous" -> p.previous.map(JsString(_)).getOrElse(JsNull),
            "next" -> p.next.map(JsString(_)).getOrElse(JsNull),
            "results" -> p.results.toJson
        )

        override def read(json: JsValue): FakePosition = deserializationError("No need to read")
    }
}

case class FakePosition(previous: Option[String], next: Option[String], results: Array[Map[String, String]]) {
}
