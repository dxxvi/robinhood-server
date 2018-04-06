package home

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import spray.json._

object RobinhoodOrder extends DefaultJsonProtocol {
    implicit object RobinhoodOrderJsonFormat extends RootJsonFormat[RobinhoodOrder] {
        override def read(json: JsValue): RobinhoodOrder = deserializationError("I don't need this")

        override def write(o: RobinhoodOrder): JsValue = JsObject(
            "account" -> JsString("https://api.robinhood.com/accounts/5RY82436/"),
            "average_price" -> JsString(f"${o.price}%.6f"),
            "cancel" -> JsString(s"https://api.robinhood.com/orders/${o.id}/cancel/"),
            "created_at" -> JsString(o.createdAt.format(DateTimeFormatter.ISO_DATE_TIME)),
            "cumulative_quantity" -> JsString(f"${o.quantity}%.5f"),
            "executions" -> JsArray(),
            "extended_hours" -> JsBoolean(true),
            "fees" -> JsString("0.00"),
            "id" -> JsString(o.id),
            "instrument" -> JsString(o.instrumentUrl),
            "last_transaction_at" -> JsString((o.createdAt plusNanos 1612345678) format DateTimeFormatter.ISO_DATE_TIME),
            "override_day_trade_checks" -> JsBoolean(false),
            "override_dtbp_checks" -> JsBoolean(false),
            "position" -> JsString(o.instrumentUrl.replace("instruments", "positions/5RY82436")),
            "price" -> JsString(f"${o.price}%.6f"),
            "quantity" -> JsString(f"${o.quantity}%.5f"),
            "ref_id" -> JsString("4e440db9-ca17-477c-b785-f229b5f670d7"),
            "reject_reason" -> JsNull,
            "response_category" -> JsString("success"),
            "side" -> JsString(o.side),
            "state" -> JsString(o.state),
            "stop_price" -> JsNull,
            "time_in_force" -> JsString(o.timeInForce),
            "trigger" -> JsString(o.trigger),
            "type" -> JsString("limit"),
            "updated_at" -> JsString(o.updatedAt.format(DateTimeFormatter.ISO_DATE_TIME)),
            "url" -> JsString(s"https://api.robinhood.com/orders/${o.id}/")
        )
    }
}

case class RobinhoodOrder(
         updatedAt: LocalDateTime,
         timeInForce: String,
         cancelUrl: Option[String],
         id: String,
         instrumentUrl: String,
         trigger: String,
         orderType: String,
         price: Double,
         createdAt: LocalDateTime,
         side: String,
         state: String,
         quantity: Double
 )
