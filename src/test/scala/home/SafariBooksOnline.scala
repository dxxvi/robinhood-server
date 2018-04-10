package home

import java.nio.file.{Files, Paths}
import java.nio.file.StandardOpenOption._
import java.util.UUID

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.FunSuite
import spray.json.RootJsonFormat

import scala.annotation.tailrec
import scala.io.{Source, StdIn}

class SafariBooksOnline extends FunSuite {
    test("process files") {
        Seq(
            "/dev/shm/book/index-1.html"
        ).foreach(fileName => {
            val bs = Source.fromFile(fileName)

            // make <pre at the beginning of a line and </pre> at the end of a line
            var s = """<pre""".r.replaceAllIn(bs.mkString, "\n<pre")
            s = """</pre>""".r.replaceAllIn(s, "</pre>\n")

            bs.close()

            // replace all the <pre ...</pre> with <div class="pre" id="uuid..."></div> and store the replacements in file
            var replacements = Map.empty[String, Seq[String]]
            var currentUUID: Option[String] = None
            s = s.split('\n').map {
                case l1: String if l1.startsWith("<pre") =>
                    val uuid = UUID.randomUUID.toString
                    replacements = replacements + (uuid -> List(l1))
                    currentUUID = Some(uuid)
                    if (l1.endsWith("</pre>")) currentUUID = None
                    s"""<div class="pre" id="$uuid"></div>"""
                case l2: String if l2.endsWith("</pre>") =>
                    currentUUID.flatMap(replacements.get(_))
                            .foreach(list => replacements = replacements + (currentUUID.get -> (list :+ l2)))
                    currentUUID = None
                    ""
                case l3: String => currentUUID match {
                    case Some(uuid) =>
                        replacements = replacements + (uuid -> (replacements(uuid) :+ l3))
                        ""
                    case None => l3
                }
            }.mkString("\n")

            val outputSettings = new Document.OutputSettings().indentAmount(2).prettyPrint(true).charset("UTF-8")
            // beautify
            val document = Jsoup.parse(s).outputSettings(outputSettings)
            // forPackt(document)

            val regex = """^([ ]+)<div class="pre" id="([a-z\d]{8}-[a-z\d]{4}-[a-z\d]{4}-[a-z\d]{4}-[a-z\d]{12})"></div>[ ]*$""".r
            // put the <pre's back
            s = document.outerHtml.split('\n').map {
                case regex(space, uuid) =>
                    space + replacements.getOrElse(uuid, Seq("""<div class="not-found"></div>""")).mkString("\n")
                case l => l
            }.mkString("\n")
            Files.write(Paths.get(fileName), s.getBytes, CREATE, TRUNCATE_EXISTING)
        })
    }

    // fix the Table of Contents
    private def forPackt(document: Document): Unit = {
        val regex = """([a-z\d]{8}-[a-z\d]{4}-[a-z\d]{4}-[a-z\d]{4}-[a-z\d]{12})\.html""".r
        val toc = toOption[Element](document.getElementById("toc"))
        toc.map(t => (t, t.getElementsByTag("li"))).foreach(tuple => tuple._2.forEach(li => {
            li.addClass(s"level${distance(li, tuple._1) / 2}")  // .removeAttr("style")
        }))
        toc.map(t => t.getElementsByTag("a")).foreach(_.forEach(e => {
            val id = e.attr("href").replace(".xhtml", "")
            e.attr("href", s"#$id")
            val levelClass = e.parent.className
            val title = e.text

            document.getElementsByTag("h1").forEach(h1 => {
                if (!h1.className.contains("level") && title == h1.text) {
                    h1.removeClass("header-title").addClass(levelClass).attr("id", id)
                }
            })
        }))
    }

    private def distance(e1: Element, e2: Element): Int = distance(e1, e2, 0)

    @tailrec
    private def distance(e1: Element, e2: Element, d: Int): Int = {
        if (e1 == null || e2 == null)
            Int.MaxValue
        else if (e1 == e2)
            d
        else {
            distance(e1.parent(), e2, d + 1)
        }
    }

    private def toOption[T](x: Any): Option[T] = if (x == null) None else Some(x.asInstanceOf[T])

}

case class Payload(text: String)

object MyJsonProtocol extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
        with spray.json.DefaultJsonProtocol {
    implicit val payLoadFormat: RootJsonFormat[Payload] = jsonFormat1(Payload.apply)
}
