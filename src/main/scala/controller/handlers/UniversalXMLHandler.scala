package controller.handlers

import _root_.entity.{HistoryTrade, SecurityHub}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import config.AppServer.{db, historiesTradeTable, securitiesHubTable}
import slick.jdbc.PostgresProfile.api._

import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class UniversalXMLHandler {

  /**
   * Универсальный обработчик для парсинга полученных данных из GET-запросов, или файлов.
   */

  def xmlParsing(rows: Seq[NodeSeq])(implicit ec: ExecutionContext): Unit = {

    val attributeValues = rows.map { row =>
      val secid = (row \ "@SECID").text
      val regnumber = (row \ "@REGNUMBER").text
      val shortname = (row \ "@SHORTNAME").text
      val emitentTitle = (row \ "@EMITENTTITLE").text
      val tradedate = (row \ "@TRADEDATE").text
      val numtrades = (row \ "@NUMTRADES").text
      val openString = (row \ "@OPEN").text
      val open = if (openString.nonEmpty) openString.toDouble else 0.0
      val closeString = (row \ "@CLOSE").text
      val close = if (closeString.nonEmpty) closeString.toDouble else 0.0

      List(secid, regnumber, shortname, emitentTitle, tradedate, numtrades, open.toString, close.toString)
    }

    val securities = rows.map { row =>
      val secid = (row \ "@SECID").text
      val regnumber = (row \ "@REGNUMBER").text
      val shortname = (row \ "@SHORTNAME").text
      val emitentTitle = (row \ "@EMITENTTITLE").text

      SecurityHub(
        id = None,
        secid = secid,
        regnumber = regnumber,
        name = shortname,
        emitentTitle = emitentTitle
      )
    }

    val securitiesQuery = securitiesHubTable ++= securities
    val securitiesResult = db.run(securitiesQuery).map(_ => "Insertion into securitiesHubTable successful")(ec)

    val historyTrades = rows.map { row =>
      val id = None
      val secid = (row \ "@SECID").text
      val tradeDateStr = (row \ "@TRADEDATE").text
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val tradedate = if (tradeDateStr.isEmpty) None else Some(Date.valueOf(LocalDate.parse(tradeDateStr, formatter)))
      val numtrades = (row \ "@NUMTRADES").text
      val openString = (row \ "@OPEN").text
      val open = if (openString.nonEmpty) openString.toDouble else 0.0
      val closeString = (row \ "@CLOSE").text
      val close = if (closeString.nonEmpty) closeString.toDouble else 0.0

      HistoryTrade(
        id = None,
        secid = secid,
        tradedate = tradedate,
        numtrades = numtrades,
        open = open,
        close = close
      )
    }
    val historyTradesQuery = historiesTradeTable ++= historyTrades
    val historyTradesResult = db.run(historyTradesQuery).map(_ => "Insertion into historiesTradeTable successful")(ec)

    securitiesResult.flatMap { securitiesMsg =>
      historyTradesResult.map { historyTradesMsg =>
        println(securitiesMsg)
        println(historyTradesMsg)
      }(ec)
    }(ec).recover { case ex =>
      println(s"Error inserting data: ${ex.getMessage}")
    }(ec).onComplete(_ => db.close())(ec)
  }

  implicit val xmlUnmarshaller: FromEntityUnmarshaller[NodeSeq] =
    Unmarshaller.stringUnmarshaller.map { str =>
      scala.xml.XML.loadString(str)
    }
}
