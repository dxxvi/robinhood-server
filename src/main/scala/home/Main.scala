package home

import language.postfixOps
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpRequest, HttpResponse, ResponseEntity, StatusCodes, Uri}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.io.StdIn
import java.io._
import java.net.InetSocketAddress
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.settings.ConnectionPoolSettings
import org.apache.tika.Tika
import spray.json._

import scala.annotation.tailrec
import scala.sys.SystemProperties
import scala.util.Random

object Main extends SprayJsonSupport with DefaultJsonProtocol {
    private val GITHUB = "https://raw.githubusercontent.com/dxxvi/stock-quotes/master/"
    private val random = new Random

    def main(args: Array[String]) {
        implicit val system: ActorSystem = ActorSystem("R")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher

        val systemProperties = new SystemProperties
        val proxyHost: Option[String] = systemProperties.get("https.proxyHost")
        val proxyPort: Option[String] = systemProperties.get("https.proxyPort")
        val connectionSettings: Option[ConnectionPoolSettings] = for {
            ph <- proxyHost
            pp <- proxyPort map (_.toInt)
        } yield ConnectionPoolSettings(system).withTransport(ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(ph, pp)))

        val symbols = systemProperties.get("symbols").get
        val year    = systemProperties.get("year").map(_.toInt).get
        val month   = systemProperties.get("month").map(_.toInt).get
        val date    = systemProperties.get("date").map(_.toInt).get
        val x: Seq[Future[(String, Array[Quote])]] = symbols.split(",")
                .map(symbol => Future {
                    (symbol, getQuotes(year, month, date, symbol, connectionSettings, system, materializer))
                }).toSeq
        val symbol2Quotes: Map[String, Array[Quote]] = Await.result(Future.sequence(x).map(_.toArray), 49 seconds).toMap
        val timeOffset = LocalTime.parse(systemProperties.get("start").get, DateTimeFormatter.ISO_LOCAL_TIME)
                .until(LocalTime.now, ChronoUnit.SECONDS)

        val tika = new Tika

        val currentDirectory = new File(".").getAbsolutePath

        val route: Route = {
            path("api-token-auth") {
                post {
                    complete(HttpResponse(
                        status = StatusCodes.MovedPermanently,
                        headers = collection.immutable.Seq(Location("/api-token-auth/"))
                    ))
                }
            } ~
            path("api-token-auth"/) {
                post {
                    complete(Map("token" -> "01234"))  // 9e1f1f22e7e2cdc3fe0373529e8194aa9b3a8d9d
                }
            } ~
            path("quotes"/) {
                get {
                    parameter('symbols) { symbols => {
                        val time = LocalTime.now.minusSeconds(timeOffset)
                        println(s"Get data at $time")
                        symbols.split(",")
                                .map(symbol => (symbol, symbol2Quotes.get(symbol)))
                                .collect {
                                    case (symbol, Some(quotes)) => (symbol, getQuote(quotes, time))
                                }
                                .foreach(println)

                        complete(Map("your symbols are" -> symbols))
                    }}
                }
            } ~
            path("orders"/) {
                get {
                    parameters(Symbol("updated_at[gte]").?) { ua =>
                        complete(s"updated_at[gte] is $ua")
                    }
                }
            } ~
            path("positions"/) {
                get {
                    complete(createFakePositions)
                }
            } ~
            path("portfolios"/) { get { complete(createFakePortfolios) } }
        }
/*
            get {
                entity(as[HttpRequest]) { httpRequest =>
                    complete {
                        val fileName = httpRequest.uri.path.toString.takeWhile(_ != '?') match {
                            case x if x == "" || x == "/" => "index.html"
                            case y: String => y
                        }
                        val path = Paths.get(currentDirectory, "src/main/resources/static", fileName)
                        if (Files.exists(path)) {
                            val mime = tika.detect(path)
                            ContentType.parse(mime) match {
                                case Right(contentType) =>
                                    HttpResponse(StatusCodes.OK, entity = HttpEntity(contentType, Files.readAllBytes(path)))
                                case Left(errors) => HttpResponse(
                                    StatusCodes.InternalServerError,
                                    entity = s"Can't determine MIME type: ${errors.mkString(" ")}"
                                )
                            }
                        }
                        else {
                            HttpResponse(StatusCodes.NotFound, entity = s"${path.toString} is not found")
                        }
                    }
                }
            }
*/

        val port = 8282
        val bindingFuture = Http().bindAndHandle(route, "localhost", port)
        println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
        StdIn.readLine()                              // let it run until user presses return
        bindingFuture
                .flatMap(_.unbind())                  // trigger unbinding from the port
                .onComplete(_ => system.terminate())  // and shutdown when done
    }

    private def getQuotes(year: Int, month: Int, date: Int, symbol: String,
                          connectionSettings: Option[ConnectionPoolSettings],
                          _system: ActorSystem, _materializer: ActorMaterializer): Array[Quote] = {
        import Quote._
        implicit val system: ActorSystem = _system
        implicit val materializer: ActorMaterializer = _materializer
        implicit val executionContext: ExecutionContextExecutor = _system.dispatcher

        val uri = f"$GITHUB%s/${symbol.toUpperCase}%s-quotes-$year%d-$month%02d-$date%02d.json"
        val hrFuture = if (connectionSettings.nonEmpty)
            Http().singleRequest(HttpRequest(GET, Uri(uri)), settings = connectionSettings.get)
        else
            Http().singleRequest(HttpRequest(GET, Uri(uri)))

        val qArrayFuture = hrFuture.flatMap(hr =>
            if (hr.status == StatusCodes.OK)
                hr.entity.toStrict(29 seconds).map(_.getData.utf8String.parseJson.convertTo[Array[Quote]])
            else
                Future(Array.empty[Quote])
        )
        Await.result(qArrayFuture, 29 seconds)
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

    private def createFakePortfolios: Map[String, Array[Map[String, String]]] =
        Map("results" -> Array(Map(
            "account" -> "https://api.robinhood.com/accounts/5RY82436/",
            "adjusted_equity_previous_close" -> "24263.3000",
            "equity" -> s"${24678.9100 + random.nextInt(2000)/100.0 - 10}",
            "equity_previous_close" -> "24263.3000",
            "excess_maintenance" -> "19811.2370",
            "excess_maintenance_with_uncleared_deposits" -> "19811.2370",
            "excess_margin" -> "19054.3476",
            "excess_margin_with_uncleared_deposits" -> "19054.3476",
            "extended_hours_equity" -> "24678.7100",
            "extended_hours_market_value" -> "8400.0900",
            "last_core_equity" -> "24678.9100",
            "last_core_market_value" -> "8400.2900",
            "market_value" -> "8400.2900",
            "start_date" -> "2017-02-16",
            "unwithdrawable_deposits" -> "0.0000",
            "unwithdrawable_grants" -> "0.0000",
            "url" -> "https://api.robinhood.com/portfolios/5RY82436/",
            "withdrawable_amount" -> "18278.6200"
        )))

    private def createFakePositions: Map[String, Any] =
        Map(
            "previous" -> None,
            "next" -> "http://localhost ..."
        )
}