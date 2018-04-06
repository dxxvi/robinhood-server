package home

import java.time.{LocalDateTime, LocalTime}

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

import scala.annotation.tailrec
import scala.io.Source

class RouteTests extends FunSuite with Matchers with ScalatestRouteTest {
    test("binary search") {
        import Quote._
        import spray.json._

        val quotes = Source.fromFile("/home/ly/HTZ-quotes-2018-02-07.json").mkString.parseJson.convertTo[Array[Quote]]
        println(getQuote(quotes, LocalTime.of(9, 22, 43)))

        println(getQuote(quotes, LocalTime.of(9, 32, 44)))

        println(getQuote(quotes, LocalTime.of(9, 33, 12)))
    }

    private def getQuote(quotes: Array[Quote], time: LocalTime): Option[Quote] =
        if (quotes.isEmpty) None else getQuote(quotes, time, 0, quotes.length - 1)

    @tailrec
    private def getQuote(quotes: Array[Quote], time: LocalTime, i1: Int, i2: Int): Option[Quote] = {
        i2 - i1 match {
            case x if x < 0 => None
            case 0 => if (quotes(i1).ldt.toLocalTime.isAfter(time)) None else Some(quotes(i1))
            case 1 =>
                if (quotes(i1).ldt.toLocalTime.isAfter(time))
                    None
                else if (quotes(i2).ldt.toLocalTime.isAfter(time))
                    Some(quotes(i1))
                else
                    Some(quotes(i2))
            case _ =>
                val i = (i1 + i2) / 2
                if (quotes(i).ldt.toLocalTime.isAfter(time))
                    getQuote(quotes, time, i1, i - 1)
                else
                    getQuote(quotes, time, i, i2)
        }
    }
}
