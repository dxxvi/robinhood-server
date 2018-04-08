package home

import spray.json._

object Order extends DefaultJsonProtocol {
    implicit object OrderJsonFormat extends RootJsonFormat[Order] {
        override def read(json: JsValue): Order = {
            val fields = json.asJsObject("check our order json string").fields
            val instrumentUrl = fields.get("instrument").collect({ case JsString(x) => x }).head
            val symbol = fields.get("symbol").collect({ case JsString(x) => x }).head
            val orderType = fields.get("type").collect({ case JsString(x) => x }).head
            val timeInForce = fields.get("time_in_force").collect({ case JsString(x) => x }).head
            val trigger = fields.get("trigger").collect({ case JsString(x) => x }).head
            val price = fields.get("price").collect({ case JsNumber(x) => x.toDouble }).head
            val quantity = fields.get("quantity").collect({ case JsNumber(x) => x.toInt }).head
            val side = fields.get("side").collect({ case JsString(x) => x }).head

            if (!Set("market", "limit").contains(orderType)) {
                deserializationError(s"type in ${json.prettyPrint} must be market or limit")
            }
            else if (!Set("gfd", "gtc", "ioc", "opg").contains(timeInForce)) {
                deserializationError(s"time_in_force in ${json.prettyPrint} must be gfd, gtc, ioc or opg")
            }
            else if (!Set("immediate", "stop").contains(trigger)) {
                deserializationError(s"trigger in ${json.prettyPrint} must be immediate or stop")
            }
            else if (!Set("buy", "sell").contains(side)) {
                deserializationError(s"side in ${json.prettyPrint} must be buy or sell")
            }
            else {
                Order(instrumentUrl, symbol, orderType, timeInForce, trigger, price, quantity, side)
            }
        }

        override def write(obj: Order): JsValue = serializationError("Do I need this?")
    }
}

case class Order(instrumentUrl: String, symbol: String, orderType: String, timeInForce: String,
                 trigger: String, price: Double, quantity: Int, side: String)
